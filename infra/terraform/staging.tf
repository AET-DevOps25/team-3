terraform {
  backend "s3" {
    bucket         = "t3-tf-state"
    key            = "infra/staging/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "terraform-locks"
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

# Staging instance - smaller instance type for cost optimization
resource "aws_instance" "team-3-staging-server" {
  ami                    = "ami-084568db4383264d4"
  instance_type          = "t2.micro"
  key_name               = "vockey"
  vpc_security_group_ids = [aws_security_group.team_3_staging_sg.id]

  tags = {
    Name = "Team-3 Staging Server"
    Environment = "staging"
    AutoDelete = "true"  # Tag for automatic cleanup
  }

  # User data script for initial setup
  user_data = <<-EOF
              #!/bin/bash
              yum update -y
              yum install -y docker git
              systemctl start docker
              systemctl enable docker
              usermod -a -G docker ec2-user
              curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
              chmod +x /usr/local/bin/docker-compose
              EOF
}

# Elastic IP for staging
resource "aws_eip" "team_3_staging_eip" {
  instance = aws_instance.team-3-staging-server.id
  tags = {
    Name = "Team-3 Staging Server EIP"
    Environment = "staging"
    AutoDelete = "true"
  }
}

# Security Group for staging
resource "aws_security_group" "team_3_staging_sg" {
  name        = "team-3-staging-sg"
  description = "Security group for Team-3 staging environment"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Traefik Dashboard"
    from_port   = 8082
    to_port     = 8082
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Additional ports for staging services
  ingress {
    description = "Client App"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Server API"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "GenAI Service"
    from_port   = 8000
    to_port     = 8000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "team-3-staging-sg"
    Environment = "staging"
  }
}

data "aws_vpc" "default" {
  default = true
}

# Outputs for staging
output "staging_public_ip" {
  value = aws_eip.team_3_staging_eip.public_ip
}

output "staging_instance_id" {
  value = aws_instance.team-3-staging-server.id
} 