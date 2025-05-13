resource "null_resource" "save_outputs" {
  provisioner "local-exec" {
    command = <<EOT
    echo "sqs_queue_name: ${aws_sqs_queue.queue.name}" >> "${path.module}/output.txt"
    echo "sqs_queue_url: ${aws_sqs_queue_policy.sqs_policy.queue_url}" >> "${path.module}/output.txt"
    EOT
  }
depends_on = [aws_sqs_queue.queue, aws_sqs_queue_policy.sqs_policy]
}


# resource "aws_s3_bucket_object" "object" {
#   bucket = "INFRABUCKET"
#   key    = "cm_values_file"
#   source = "VALUESFILENAME"

#   # The filemd5() function is available in Terraform 0.11.12 and later
#   # For Terraform 0.11.11 and earlier, use the md5() function and the file() function:
#   # etag = "${md5(file("path/to/file"))}"
#   etag = filemd5("path/to/file")
# }
