# Local Authentication

1. Login with the gcloud CLI to obtain credentials locally. Make sure the account you choose has access to your desired agent.
```
gcloud auth login
```

2. Set your project
```
gcloud config set project <project-name>
```

3. Set up application default credentials for use by local applications.
```
gcloud auth application-default login
```