# Ritzel - a Pulumi Backend

This is a project to implement a Pulumi http backend and it is not yet working.

## Why

Pulumi is a great concept and I will always prefer to write some code over writing yaml.  You can use Pulumi with a local file backend, with cloud blob stores and with the proprietary pulumi service. All of them are fine but with a team working on one infra you need to store the current state centrally and do some locking to avoid concurrent changes on one resource.

Besides that, it is good to have access control of some kind, a nice history that you can scroll through, centrally managed configuration and a reliable backup of your checkpoints. This is basically [what Pulumi offers with their backend](https://www.pulumi.com/docs/intro/concepts/state/#pulumi-service-backend-features) service. And this is what I would love to see as features in this project.

## Development Workflow

### Requirements

- [Install Pulumi CLI](https://www.pulumi.com/docs/get-started/install/)
- [Install Clojure](https://clojure.org/guides/getting_started)
- [Install Leiningen](https://leiningen.org/#install)

### Workflow

To get all the API calls Pulumi does you can set `-v=11` on every invocation. This writes the logs including API calls into a file in `/tmp`. To get to know the Pulumi service API you should have a look at the [incomplete list of endpoints](https://github.com/pulumi/pulumi/blob/master/pkg/backend/httpstate/client/api_endpoints.go) and the [actual client implementations](https://github.com/pulumi/pulumi/blob/master/pkg/backend/httpstate/client/client.go). For further details on what data the client sends and receives please refer to the folder `sdk/go/common/apitype` in the [pulumi cli source code](https://github.com/pulumi/pulumi/tree/master/sdk/go/common/apitype).

### Authentication

The authentication between Pulumi and its backend luckily works with a simple token. You can get your token on the website and use it with your http client of choice to run the requests by hand and compare them with Ritzel. Start your Ritzel with `lein run` and run e.g. httpie against it like this `http :3000/api/user "authorization: token 2f904e245c1f5`. The token is currently hardcoded as this project is in a very early stage.

It seems to me that Pulumi CLI does not retrieve a lot of data from the backend so it is not necessary in this early stage to save all the checkpoints and event-batches. It might become more important when a user interface is more important and the correct behaviour is implemented. But now I want to show that it is actually easy to implement the basics for an MVP.

### API spec

Most of the API is already specced. I need to go over the client.go again to complete the last endpoints but most of it can already be found in the swagger.yml. For this to see you need to run Ritzel with `lein run` and fetch it at `http://localhost:3000/api/swagger.yml`.

### MVP

- [x] Login
- [x] Stack Init
- Up
- Destroy
- Stack Remove

### Data Definition

Please see some payloads in the folder `doc/` to get an understanding of the most important ones. There will be a more sophisticated data definition as soon as I implemented the datalog schema and have a proper understanding of what is necessary to build it even further.

### Contributing

Mostly the handlers with the necessary datalog bits need to be implemented right now. Most of it is writing something to the database, some querying is also necessary e.g. for the stack listing. Right now Datahike runs with schema on read so you can dump your data the way you want. I made a lot of TODO annotations to the source code and a bigger implementation is the locking mechanism, but even that can be mocked for the MVP.

## Architecture

![Pulumi backend architecture](https://www.pulumi.com/images/docs/reference/state_saas.png)
