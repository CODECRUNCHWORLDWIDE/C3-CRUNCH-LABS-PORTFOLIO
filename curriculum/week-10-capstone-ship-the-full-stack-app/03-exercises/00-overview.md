# Week 10 — Exercises

Three exercises, in the order you do them. They are the proof-of-readiness steps that gate the capstone delivery: you cannot deploy a system you have not containerized, you cannot have CI/CD without a pipeline, and you cannot demo a mobile build that points at `localhost`. Do them against your *real* Crunch Tracker (the one you've compounded since Week 01), not a toy.

| # | File | What you do | Est. time |
|---|------|-------------|-----------|
| 1 | [exercise-01-multi-stage-dockerfile.md](./exercise-01-multi-stage-dockerfile.md) | Write a multi-stage Dockerfile for the Spring Boot API, build a small non-root image, and run it against a managed (or local) Postgres with config from the environment. | ~90 min |
| 2 | [exercise-02-deploy-workflow.yml](./exercise-02-deploy-workflow.yml) | A complete GitHub Actions pipeline: test on every PR; on `main`, build, scan, push to GHCR, and deploy. Drop it in, set the secrets, push. | ~90 min |
| 3 | [exercise-03-app-config.ts](./exercise-03-app-config.ts) | The typed Expo config module that reads the API URL from build-time config, so the EAS build points at the live API and fails loudly if it isn't set. | ~60 min |

## Rules

- **Exercise 1 is a guided Markdown walkthrough** with the full Dockerfile and the build/run commands, because the work is operational (writing a Dockerfile, running `docker build`, pointing at a database) rather than a single program.
- **Exercise 2 is a real, runnable GitHub Actions workflow.** Copy it to `.github/workflows/deploy.yml` in your capstone repo, set the documented secrets, and push — it runs. It is valid YAML against the 2026 action versions; lint it with `actionlint` if you have it.
- **Exercise 3 is real, runnable TypeScript.** It compiles under the Expo + TypeScript toolchain (`tsc --noEmit` is clean) and is the actual config seam your mobile app uses. Read it, drop it in `config/env.ts`, and wire your `apiClient` to it.
- Install once for the API side: Docker (Engine + Buildx). For the pipeline: the `gh` CLI and a GHCR-enabled repo. For the mobile side: `npm install -g eas-cli` and a free Expo account.
- Set the secrets the workflow needs before you push: `gh secret set RENDER_DEPLOY_HOOK` (or your host's equivalent). The image push uses the built-in `GITHUB_TOKEN`, so there is no registry credential to manage.

## The bar

You have cleared the exercises when:

- **Exercise 1** produces an image under ~250MB that runs as a non-root user, comes up healthy against Postgres with *all* config from environment variables, and Flyway builds the schema from empty. `git grep` finds no real secret in `application.yml`.
- **Exercise 2** runs green on a PR (tests only) and, on a push to `main`, builds + scans + pushes the image to `ghcr.io/<owner>/<repo>` and triggers the deploy — with no long-lived credential in the repo.
- **Exercise 3** compiles clean, throws a clear error when `EXPO_PUBLIC_API_URL` is unset, and resolves to your live API URL when built with the `preview` profile.

The full solution for Exercise 1 is inline (it is operational). Exercises 2 and 3 *are* the solution — complete, correct files you adapt to your own host and API URL, not skeletons with TODOs.

## How these compound into the capstone

Exercise 1's image is what the mini-project deploys. Exercise 2's pipeline is the "one documented pipeline run" the Challenge demands. Exercise 3's config seam is what makes the EAS build in the mini-project point at the live API instead of your laptop. Do the three and most of the capstone's machinery exists; the mini-project is assembling and proving them end to end.
