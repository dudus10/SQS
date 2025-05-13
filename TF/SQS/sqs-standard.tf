
resource "aws_sqs_queue" "queue" {
  name = var.sqs_name != "" ? var.sqs_name : format("%s%s", "sqs-", random_string.special.result )
  delay_seconds              = 10
  visibility_timeout_seconds = 30
  max_message_size           = 2048
  message_retention_seconds  = 86400
  receive_wait_time_seconds  = 2
  sqs_managed_sse_enabled = true

  depends_on = [random_string.special]

}

resource "random_string" "special" {
  length  = 8
  upper   = false
  lower   = false
  numeric  = true
  special = false
}


resource "aws_sqs_queue_policy" "sqs_policy" {
  queue_url = aws_sqs_queue.queue.id
  policy    = data.aws_iam_policy_document.sqs_policy.json
}

output "sqs_name" {
  value = aws_sqs_queue.queue.name
}

