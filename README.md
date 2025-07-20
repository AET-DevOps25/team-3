# 📚 Study Mate – AI-Powered Study Companion

**Study Mate** is an AI-powered learning tool that helps students better understand study materials. Given a slide deck or document, it generates:

- ✅ Summaries  
- 🧠 Flashcards  
- ❓ Quizzes  
- 💬 A subject-matter chatbot  

Built using **RAG (Retrieval-Augmented Generation)**, it enhances learning with context-aware responses grounded in your own study materials.


## 🧱 Architecture Overview

The system is composed of several modular components:

- **Client-side**: A modern web interface (`client/`)
- **Server-side**: Kotlin Spring Boot microservices
  - `auth-service`
  - `document-service`
  - `genai-service`
  - `eureka-server`
  - and shared modules
- **Infrastructure**:
  - **PostgreSQL** for structured data - Documents, users, etc.
  - **Weaviate** as a vector store for RAG semantic search
  - **Traefik** for routing
  - **Prometheus** & **Grafana** for observability (`monitoring/`)
  - **Terraform** for AWS setup (EC2 instance provisioning, Security groups, EIP, etc.)
  - **Ansible** for configuring EC2 instances, Docker config validation, and EC2 deployment.

> 📄 See [MICROSERVICES_README.md](./server/MICROSERVICES_README.md) for more on each server-side service.


## 🚀 Deployments

We support two deployment strategies, both using images hosted on **GitHub Container Registry (GHCR)**:

### 1. AWS (EC2-based)

Provisioned and managed using:

- **Terraform**: to create an EC2 instance with an Elastic IP, EBS volume, VPC, and a custom security group
- **Ansible**: to SSH into the instance, install Docker, and prepare the environment

### 2. Kubernetes

- Uses **Helm charts** stored under `infra/helm/`
- Deploys to the Kubernetes cluster on **Rancher**

## 🔄 CI/CD Pipeline Overview

This project uses GitHub Actions for CI/CD. Below is a summary of each pipeline defined in the `.github/workflows/` directory.


#### ✅ Build & Validation – `build-validation.yml`

This workflow runs on every pull request to ensure code quality and consistency. It includes:

- Linting
- Testing
- Docker build check

It helps catch issues early before merging into the main branch.


#### 🐳 Image Build & Push – `build-images.yml`

This workflow builds and pushes Docker images for all major components to **GitHub Container Registry (GHCR)**. It:

- Builds Docker images for each service
- Tags them with the current commit SHA or version
- Pushes them to GHCR for use in deployments

This workflow is typically triggered on merges to main or via manual dispatch.


#### ☁️ AWS Deployment – `deploy_aws.yml`

This pipeline handles deployment to the **AWS EC2 environment**. It performs the following steps:

1. Logs into GitHub Container Registry (GHCR)
2. Pulls Docker images
3. SSHs into the EC2 instance
4. Runs Docker Compose using the pulled images

This flow assumes the EC2 instance is already provisioned via **Terraform** and configured via **Ansible**.


#### ☸️ Kubernetes Deployment – `deploy-kubernetes.yml`

This workflow deploys the application to the **Kubernetes cluster** on **Rancher** using:

- `kubectl` for cluster interaction
- `helm` for templated deployments

It pulls the latest GHCR images and upgrades the cluster using Helm charts defined under `infra/helm/`.


## 📂 Repository Structure

```
.
├── client/                   # Client-side web app (Vite + Tailwind)
├── genai/                    # Python-based RAG service
├── server/                   # Kotlin Spring Boot microservices
│   ├── auth-service          # Auth and user management service
│   ├── document-service      # Document management service
│   ├── genai-service         # A middleware for the interactions between client and genai
│   ├── eureka-server         # Service discovery
│   └── shared                # Shared modules between microservices
├── infra/                    # EC configuration and provisioning, Helm
├── monitoring/               # Prometheus & Grafana config
├── traefik/                  # Reverse proxy setup
├── ansible/                  # Validation and Deployment playbooks
├── system_overview/          # Architecture and UML diagrams
├── docker-compose.dev.yml    # Dev Docker compose for local deployment
├── *.md                      # Docs and setup guides
.
```


## 🧪 Quick Start (Local Dev)

1. **Clone the repo**:
   ```bash
   git clone https://github.com/AET-DevOps25/team-3.git
   cd team-3
   ```

3. **Start the system**:
   ```bash
   ./start-dev
   ```
   The `start-dev` script supports the following flags:

   | Flag | Description                              | Docker Equivalent                |
   |------|------------------------------------------|----------------------------------|
   | `-r` | Force recreate containers                | `--force-recreate`              |
   | `-d` | Run in detached mode (background)        | `-d`                            |
   | `-b` | Build the images        | `--build`                            |
4. Visit `http://localhost` to start using Study Mate.
5. To shut down the system run:
   ```bash
   ./stop-dev
   ```


## 👥 Contributors

- [@yassinsws](https://github.com/yassinsws) – Microservices, Kubernetes deployment
- [@marvin-heinrich](https://github.com/marvin-heinrich) – Client-side, Testing
- [@waleedbaroudi](https://github.com/waleedbaroudi) – GenAI, Monitoring, AWS Deployment



## 📄 License

This project is licensed under the terms of the [MIT License](LICENSE).
