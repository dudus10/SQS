// import java.text.SimpleDateFormat 
// import java.util.Date
import java.time.LocalDateTime

def dt = LocalDateTime.now()
def bucketname //"backet${timestamp}"
def tfstatebucket = ""
def tfstatepath = ""
def bucket_name = ""
def ignore_backend = ""
		                
pipeline {
    // timestamp = Date().getTime()

    environment { 
        
        AWS_ACCESS_KEY_ID = credentials('AWS_ACCESS_KEY_ID')
        AWS_SECRET_ACCESS_KEY = credentials('AWS_SECRET_ACCESS_KEY')
        AWS_DEFAULT_REGION = "eu-west-1"
        
        // TF_VAR_bucket_name = ""
        //bucketparam = ""
    }    
    
    parameters {
        string(name: 'tf_state_bucket', defaultValue: '', description: "")
        string(name: 'tf_state_path', defaultValue: '', description: "")
        string(name: 'bucket_name', defaultValue: '', description: "")
        booleanParam(defaultValue: false, description: 'Ignore backend - create TF state_file on exisitng bucket True/False', name: 'ignore_backend')
    }
    
	agent any

	stages {
      
        
	    stage('Cloning repo from github') {
            steps {

		        sh 'rm -rf TF/S3'
		    
                git branch: 'main', url: 'git@github.com:dudus10/SQS_private.git'

            }
        }
        

		stage('Create S3 Bucket') {
		    steps {
		        script {
		            print(dt)
		            dir("TF/S3") {
		                
		                if (params.ignore_backend == true) {
		                    sh "python3 remove_backend.py && rm -f creates3bucket_state.tf"
							sh "touch ignore_backend"
		                    //ignore_backend = '-backend=false'
		                } else {

    		                if (params.tf_state_bucket != '') {
    		                    //tfstatebucket = params.tf_state_bucket
    		                    tfstatebucket = '-backend-config=\'bucket=' + params.tf_state_bucket + '\' '
    		                }
    		                
    		                if (params.tf_state_path != '') {
    		                    //tfstatepath = params.tf_state_path
    		                    tfstatepath = '-backend-config=\'key=' + params.tf_state_path + '\' '
    		                }
		                }

		                if (params.bucket_name == '') {
		                    error('You must provide S3 bucket name!')
		                } else {
                       		    print('Bucket name is: ' + params.bucket_name)
		        	    bucket_name = "-var='bucket_name=" + params.bucket_name + "'"
		                }

				sh """#!/bin/bash -x
							echo 'tf_state_bucket - ${tfstatebucket}'
							echo 'tf_state_path - ${tfstatepath}'
							#echo 'ignore_backend - ${ignore_backend}'
							terraform init ${tfstatepath} ${tfstatebucket} 
							echo ${bucket_name}
							terraform plan ${bucket_name}
							terraform apply -auto-approve ${bucket_name}
						   	pwd
							ls -ltra
							if ! [ -f ignore_backend ]; then
								echo "ignore_backend not found, creating bucket_values_${env.BUILD_NUMBER}.json"
								terraform output -json > bucket_values_${env.BUILD_NUMBER}.json
								ls -ltra
								aws s3 cp bucket_values_${env.BUILD_NUMBER}.json s3://${params.tf_state_bucket}/cm-values/bucket_values_${env.BUILD_NUMBER}.json
							fi
		              """
		            }
		        }
		    }
		}
		
		

		
	}
	    post {
			success {
					script {
					sh """#!/bin/bash -x
							echo "Clean workspace"
							pwd
							cd TF/S3
							ls
							rm *
							ls
						"""
					}
			}
    }
}

