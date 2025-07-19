# 🚀 Development Environment Setup


## 📦 Prerequisites

- [Docker](https://www.docker.com/)
- [Docker Compose](https://docs.docker.com/compose/)

## ▶️ Start the Dev Environment

Use the helper script to start your local environment:

```bash
./start-dev
```

This runs:

```bash
docker compose -f docker-compose.dev-no-traefik.yml up
```

### 🔧 Available Flags

The `start-dev` script supports the following flags:

| Flag | Description                              | Docker Equivalent                |
|------|------------------------------------------|----------------------------------|
| `-r` | Force recreate containers                | `--force-recreate`              |
| `-d` | Run in detached mode (background)        | `-d`                            |

#### Examples

Start with force recreate:

```bash
./start-dev -r
```

Start in detached mode:

```bash
./start-dev -d
```

Start with both flags:

```bash
./start-dev -r -d
```

### Stop the  Dev Environment

Use the helper script to stop your local environment:

```bash
./stop-dev
```# Test commit to trigger workflows
