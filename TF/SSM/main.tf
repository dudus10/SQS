terraform {
  backend "s3" {		
    # bucket = "terraform-state-dudus-homelab"
    # key    = "ssm/ssm.tfstate"
    region = "eu-west-1"
  }
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

resource "random_string" "special" {
  length  = 10
  upper   = true
  lower   = true
  numeric  = true
  special = true
}

resource "random_string" "name" {
  length  = 8
  upper   = true
  lower   = true
  numeric  = true
  special = false
}


resource "aws_ssm_parameter" "ssm_param" {
  name  = var.param_name != "" ? var.param_name : format("%s%s", "param-", random_string.name.result)
  type  = "String"
  value = var.param_value != "" ? var.param_value : random_string.special.result
}

output "param_name" {
  #value = aws_ssm_parameter.ssm_param.name
  sensitive = false
  value = aws_ssm_parameter.ssm_param.name
}
output "param_value" {
  #value = aws_ssm_parameter.ssm_param.value
  #sensitive = false
  value = nonsensitive(aws_ssm_parameter.ssm_param.value)
}

# -backend-config="key=ssm/ssm11.tfstate" -backend-config="bucket=terraform-state-dudus-homelab"