# Ritzel - a Pulumi Backend

## Why

Pulumi is a great concept and I will always prefer to write some code over writing yaml.
You can use Pulumi with a local file backend, with cloud blob stores and with the 
proprietary pulumi service. All of them are fine but with a team working on one infra
you need to store the current state centrally and do some locking to avoid concurrent
changes on one resource.

Besides that, it is good to have access control of some kind, a nice history that
you can scroll through, centrally managed configuration and a reliable
backup of your checkpoints. This is basically [what Pulumi offers with their backend](https://www.pulumi.com/docs/intro/concepts/state/#pulumi-service-backend-features)
service. And this is what I would love to see as features in this project.

## MVP

- Login
- Stack Init
- Up
- Destroy
- Stack Remove
