from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from dotenv import load_dotenv
import os

from pydantic import BaseModel

from request_models import SummaryRequest
from response_models import SummaryResponse

load_dotenv()


class StudyLLM:
    
    # TODO: remove 
    dummy_knowledge = """
Amazon Web Services (AWS) is the world’s leading cloud platform, offering a vast array of tools for computing, storage, networking, security, and more. Among its hundreds of services, several are considered foundational for most cloud-based architectures.

1. Amazon EC2 (Elastic Compute Cloud) is AWS’s core computing service. It provides resizable virtual machines (instances) in the cloud, allowing developers to deploy applications with full control over the operating system, networking, and storage. EC2 supports auto-scaling, load balancing, and a wide range of instance types to match different workloads—from general-purpose web servers to compute-optimized data processing tasks.

2. Amazon S3 (Simple Storage Service) is AWS’s object storage solution, designed to store and retrieve any amount of data from anywhere on the web. It is highly durable (99.999999999% durability) and is commonly used for backups, media hosting, data lakes, and static website content. S3 also integrates with many AWS services, making it a central component of most data pipelines.

3. Amazon RDS (Relational Database Service) provides managed relational databases, such as MySQL, PostgreSQL, MariaDB, Oracle, and SQL Server. With RDS, users can focus on application logic while AWS handles maintenance tasks like backups, patching, and failover. It also supports read replicas and Multi-AZ deployments for high availability and scalability.

4. AWS Lambda offers serverless computing, allowing developers to run code in response to events (e.g., HTTP requests, file uploads, database changes) without provisioning or managing servers. Lambda functions scale automatically and are billed based on the number of requests and execution time, making them ideal for microservices, automation, and event-driven applications.

5. Amazon VPC (Virtual Private Cloud) allows users to create isolated virtual networks in the AWS cloud. With VPC, you can define IP ranges, create subnets, configure routing tables, and set up security groups and network ACLs. This gives full control over network configuration and is essential for secure cloud architectures.

6. AWS IAM (Identity and Access Management) is the backbone of AWS security. It enables you to manage users, groups, and roles, and assign fine-grained permissions to AWS resources. With IAM, you can enforce least-privilege access, enable multi-factor authentication, and set up federated access from corporate directories.

7. Amazon CloudWatch is a monitoring and observability service that provides metrics, logs, and alarms for AWS resources and applications. You can use it to track performance, troubleshoot issues, and trigger automated responses. Paired with AWS CloudTrail, which logs all API calls and account activity, these tools are critical for security auditing and operational visibility.

8. Amazon DynamoDB is a fully managed NoSQL database service known for its low latency and scalability. It's often used in gaming, IoT, and real-time data applications where high throughput and availability are critical.

9. AWS SQS (Simple Queue Service) and SNS (Simple Notification Service) support decoupled and event-driven architectures. SQS allows message queuing between services, while SNS offers publish-subscribe messaging for alerting and broadcasting messages across systems.

10. AWS CloudFormation helps you manage infrastructure as code. You can define your entire AWS architecture using YAML or JSON templates, allowing for repeatable and version-controlled infrastructure deployments.

Together, these services form the core toolkit for developers and enterprises building scalable, reliable, and secure cloud applications on AWS. By mastering them, users can architect almost any solution, from simple websites to complex, distributed systems.
"""
    
    llm = ChatOpenAI(
        model="llama3.3:latest",
        temperature=0.5,
        api_key=os.getenv("OPEN_WEBUI_API_KEY"),
        base_url="https://gpu.aet.cit.tum.de/api/"
    )
    
    def __init__(self):
        base_system_template = ("You are an expert on the information in the passage below.\n"
                                     "Use the passage as your only knowledge source, do not get info from any other source.\n"
                                    f"Passage: {self.dummy_knowledge}\n"
                                    "Your task is {task}"
                                    )
        self.base_prompt_template = ChatPromptTemplate.from_messages([
            ('system', base_system_template),
            ('human', '{input}')
        ])
    
    def _chain(self, output_model: BaseModel = None):
        """
        Construct a chain for the LLM with given configurations.
        
        Args:
            OutputModel (BaseModel, optional): A Pydantic model for structured output.
            ...
        Returns:
            RnnableSequence: The chain for the LLM.
        """
        llm = self.llm
        
        if output_model:
            llm = llm.with_structured_output(output_model)
        
        return self.base_prompt_template | llm

    
    def prompt(self, prompt: str) -> str:
        """
        Call the LLM with a given prompt.
        
        Args:
            prompt (str): The input prompt for the LLM.
        
        Returns:
            str: The response from the LLM.
        """
        task =  (
            "To answe questions based on your knowledge."
            "You can only answer questions if the answer exists in your knowledge, Otherwise you will answer 'I don't know.'\n"
            "If you're asked a question that does not relate to your knowledge, answer with 'Unrelated question'.\n"
            )
        
               
        return self._chain().invoke({
            'task':task,
            'input':prompt
            }).content

    def summarize(self, request: SummaryRequest):
        """
        Summarize the given document using the LLM.
        
        Args:
            request (SummaryRequest): The request containing summary preferences.
        
        Returns:
            str: The summary of the document.
        """
        task = "to summarize the text in your knowledge."
        return self._chain(output_model=SummaryResponse).invoke({
            'task': task,
            'input': f"summary length: {request.length.value}"
        })