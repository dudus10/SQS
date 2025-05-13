variable "sqs_name" {
  type = string
  default = ""
}

output "sqs_url" {
  value = aws_sqs_queue_policy.sqs_policy.queue_url
}


