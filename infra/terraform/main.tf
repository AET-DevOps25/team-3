terraform {
    backend "s3" {
    bucket         = "t3-tf-state"
    key            = "infra/dev/terraform.tfstate"
    region         = "us-east-1"
    use_lock_table   = true
    encrypt        = true
  }
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.16"
    }
  }

  required_version = ">= 1.2.0"
}

provider "aws" {
  region = "us-east-1"
}

resource "aws_instance" "team-3-server" {
  ami           = "ami-084568db4383264d4"
  instance_type = "t2.micro"
  key_name      = "vockey"

  tags = {
    Name = "Team-3 Server"
  }
}

output "public_ip" {
  value = aws_instance.team-3-server.public_ip
}
