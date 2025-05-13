terraform {
  backend "s3" {
    # To provide value run - terraform init -backend-config="key=<folder_name>/<file_name>.tfstate" -backend-config="bucket=<bucket_name>"
    # bucket = ""
    # key  = ""
    region = "eu-west-1"
  }
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

resource "aws_s3_bucket" "aws_new_bucket" {
  bucket = var.bucket_name
}

resource "aws_s3_bucket_versioning" "aws_new_bucket" {
  bucket = aws_s3_bucket.aws_new_bucket.id
  versioning_configuration {
    status = "Disabled"
  }
}

output "bucket_name" {
  value = aws_s3_bucket.aws_new_bucket.bucket
}
