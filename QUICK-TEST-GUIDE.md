# Quick Test Guide

## 🚀 Quick Start (TL;DR)

```bash
# 1. Run automated setup
bash setup-tests.sh

# 2. Run all tests
bash run-all-tests.sh

# 3. Run individual tests
bash run-client-tests.sh      # React tests
bash run-genai-tests.sh       # Python tests  
bash run-microservices-tests.sh  # Kotlin tests
```

## 📋 Manual Setup

### Client Tests
```bash
cd client
npm install
npm test
```

### GenAI Tests
```bash
cd genAi
pip install -r requirements.txt requirements-test.txt
python -m pytest -v
```

### Microservices Tests
```bash
cd microservices/auth-service
./gradlew test
```

## 📊 Expected Results

- **Client**: 47 tests (some may fail due to AuthContext integration)
- **GenAI**: 28/33 tests passing (84% success rate)
- **Microservices**: Framework ready for implementation

## 🔧 Prerequisites

- Node.js 18+
- Python 3.9+
- Java 17+ (for microservices)

## 📚 Full Documentation

See [README-TESTING.md](README-TESTING.md) for comprehensive instructions.

## 🆘 Quick Troubleshooting

**Client tests fail**: Clear node_modules, run `npm install`
**GenAI import errors**: Install dependencies with `pip install -r requirements.txt requirements-test.txt`
**Permission errors**: Run `chmod +x *.sh` to make scripts executable