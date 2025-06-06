terraform {
    backend "s3" {
    bucket         = "t3-tf-state"
    key            = "infra/dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table   = "terraform-locks"
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
  vpc_security_group_ids = [aws_security_group.team_3_sg.id]

  tags = {
    Name = "Team-3 Server"
  }
}

# Elastic IP
resource "aws_eip" "team_3_eip" {
  instance = aws_instance.team-3-server.id
  tags = {
    Name = "Team-3 Server EIP"
  }
}

# SG for allowing SSH
resource "aws_security_group" "team_3_sg" {
  name        = "team-3-sg"
  description = "Allow SSH"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH from anywhere"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # allows SSH from any IP - TODO: assess for security?
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

data "aws_vpc" "default" {
  default = true
}


output "public_ip" {
  value = aws_eip.team_3_eip.public_ip
}
