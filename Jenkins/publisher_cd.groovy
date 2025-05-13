def elb_url
// This Jenkins pipeline script is designed to deploy a Helm chart for an SQS publisher application to an EKS cluster.
pipeline {
    environment { 
        
        AWS_ACCESS_KEY_ID = credentials('AWS_ACCESS_KEY_ID')
        AWS_SECRET_ACCESS_KEY = credentials('AWS_SECRET_ACCESS_KEY')
        AWS_DEFAULT_REGION = "eu-west-1"
        
    }    

    parameters {
        string(name: 'eks_name', defaultValue: '', description: "EKS to deploy to")
        string(name: 'chart', defaultValue: '', description: "chart to deploy")
    }

	agent any

	stages {
	    
	    stage('Cloning repo from github') {
            steps {
                git branch: 'main', url: 'git@github.com:dudus10/SQS_private.git'
            }
        }
        
	
		stage('Deploying sqs_publisher helm chart') {
            steps {
                script {
                        dir('Microservices/sqs_publisher') {       
                        sh """#!/bin/bash -x

                            aws eks update-kubeconfig --name ${params.eks_name}
                            
                            aws s3api get-object --bucket project1105-2025-1002 --key helm_charts/sqs-publisher-1.0.51.tgz sqs-publisher-1.0.51.tgz --no-cli-pager

                            kubectl get svc 
                            kubectl get nodes -o wide

                            helm upgrade -i sqs-publisher sqs-publisher-1.0.51.tgz --wait --timeout 2m0s
                            #helm upgrade -i sqs-publisher ${params.chart} --wait --timeout 2m0s

                            kubectl get svc
                            helm ls -A
                            kubectl get pods -A
                        """
                        }
                }
            }
		}


		stage('Get ELB URL') {
            steps {
                script {

                        elb_url = sh(script: "kubectl get svc sqs-publisher -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'", returnStdout: true).trim()
                        echo "The ELB URL is: ${elb_url}"
                        }
                }
            }
		
        stage('Run tests') {
            steps {
                script {
                        sh """#!/bin/bash -x
                            res=\$(curl --silent --output /dev/null --show-error --fail -vvv -w "%{http_code}" -k  http://${elb_url}/publish_message --header 'Content-Type: application/json' --data '{"data": {"subject": "New subject", "timestream": "0_21"}, "token": "mG*!MYg;F0UU7DKkqU"}')

                            if [[ \${res} -ge "200" && \${res} -le "299" ]]; then
                                echo "**** Test PASSED! ****"
                            else
                                echo "**** Test FAILED ****"
                                exit 1
                            fi
                        """
                        }
                }
            }
		
	}
}
