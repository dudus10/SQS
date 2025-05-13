from flask import Flask, request, jsonify
import boto3
import os

app = Flask(__name__)

# Load AWS credentials from environment variables
AWS_ACCESS_KEY = os.getenv("AWS_ACCESS_KEY")
AWS_SECRET_KEY = os.getenv("AWS_SECRET_KEY")
AWS_REGION = os.getenv("AWS_REGION", "eu-west-1")
SQS_QUEUE_URL = os.getenv("SQS_QUEUE_URL")

# get an ssm client
ssm_client = boto3.client('ssm', region_name=AWS_REGION, aws_access_key_id=AWS_ACCESS_KEY, aws_secret_access_key=AWS_SECRET_KEY)

# Initialize SQS client
sqs_client = boto3.client(
    "sqs",
    aws_access_key_id=AWS_ACCESS_KEY,
    aws_secret_access_key=AWS_SECRET_KEY,
    region_name=AWS_REGION
)

@app.route("/publish_message", methods=["POST"])
def publish_message():
    data = request.get_json()

    if not data: # or "message" not in data:
        return jsonify({"error": "No data!"}), 400

    # Get the token value from the request body
    request_token = data["token"]

    # set the parameter name you want to receive, note the f-string to pass the variable to it
    param_name = f"Token"

    # get_parameter
    token_param = ssm_client.get_parameters(Names=[param_name])

    # print / return response
    print("token_param: " + str(token_param['Parameters'][0]['Value']))
    token = str(token_param['Parameters'][0]['Value'])

    if str(token) == request_token:
        try:
            response = sqs_client.send_message(
                QueueUrl=SQS_QUEUE_URL,
                MessageBody=str(data)
            )
            return jsonify({"message_id": response}), 200
        except Exception as e:
            return jsonify({"error": str(e)}), 500
    else:
        return jsonify({"error": "Invalid token"}), 400


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=15000, debug=True)