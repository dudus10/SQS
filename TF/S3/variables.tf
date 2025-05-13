resource "random_string" "special" {
  length  = 12
  upper   = true
  lower   = true
  numeric  = true
  special = false
}

variable "bucket_name" {
  type = string
  default = ""
}

locals {
  final_string = var.bucket_name != "" ? var.bucket_name : format("%s%s", "bucket-", random_string.special.result )
}

output "final_bucket_name" {
  value = local.final_string
}

