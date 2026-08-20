# Local Router

Local Router is a small inference gateway for running multiple local model backends without fighting over the same VRAM. It lets you front several OpenAI-compatible providers (like Ollama and vLLM) with one clean API surface, and when those providers share the same resource pool it makes them cooperate instead of competing.

If you are running a VRAM-constrained box, this is the pattern it helps with:

- Ollama and vLLM both use the same GPU memory pool
- one backend can be asleep while the other is serving requests
- the router locks the shared resource pool and wakes/sleeps the relevant provider automatically
- you can still call everything through one OpenAI-compatible endpoint

This project is intentionally simple: it is a local control plane and proxy around inference servers, with a tiny web UI for managing providers, model aliases, and API keys.

## Get Started

Use the included `compose.yaml` to run Local Router with its backing services.

```bash
docker compose up -d
```

Then open the local UI on port 8081 and create your providers, aliases, and API keys.

## What it does

Local Router can:

- aggregate providers from Ollama, vLLM, and generic OpenAI-compatible servers
- expose a single OpenAI-style API at `/inference/v1/`\*
- route requests by provider and model name such as provider.model
- resolve friendly model aliases like `my-phi` -> `llama3.1:8b`
- keep only one local provider active per shared resource pool
- send sleep and wake commands to backends when needed
- proxy direct provider calls under `/proxy/:providerId/`\* for debugging or custom usage
- run a management UI on port 8081 to configure providers and keys

## Why this exists

The core idea is resource coordination.

Local model runtimes like Ollama and vLLM do not automatically coordinate with each other, especially when they are competing for the same GPU memory. Local Router introduces a per-resource-pool lock:

- each provider has a resourcePool value
- if multiple providers share the same resourcePool, only one is allowed to be active at a time
- before a local request is sent, every other provider in that pool is asked to sleep
- after the request finishes, the pool lock is released

This means you can keep Ollama and vLLM both installed and available, but only one of them will actively use the GPU at a time when they share the same memory budget.

## Supported providers

The code includes three provider types:

- OLLAMA
  - checks `/` and `/api/ps`
  - unloads running models via `/api/generate` with `keep_alive: 0`
  - waits briefly for unload to complete
- VLLM
  - checks `/health` and `/is_sleeping`
  - sleeps via `/sleep?level=2`
  - wakes via `/wake_up?tags=weights`, `/collective_rpc`, `/reset_prefix_cache`, `/wake_up?tags=kv_cache`
- OPENAI
  - generic OpenAI-compatible provider proxying
  - treated as cloud-like by default, so it does not participate in the local resource lock

## Runtime behavior

The request flow is roughly:

1. a client calls /inference/v1/chat/completions or the equivalent
2. the model name is resolved through the configured alias/provider mapping
3. the router chooses the provider and checks whether it is local
4. if it is local and belongs to a resourcePool, it locks that pool
5. other providers in the pool are told to sleep
6. the selected provider is woken up if needed
7. the request is proxied through to the backend
8. when the response completes, the pool lock is released

## Ports

- API server: http://localhost:8080
- Web UI: http://localhost:8081

The API server is the main inference endpoint. The UI is a small management portal for providers, model aliases, and API keys.

> Warning: the configuration UI is intentionally not a hardened admin interface and should not be exposed to the internet or any untrusted network. It allows creation and deletion of API keys, model aliases, and provider definitions, which can grant access to local inference resources and change routing behavior. Run it on a trusted local network only, behind a firewall or reverse proxy with proper access controls.

## Authentication

The main inference routes and direct provider proxy routes require a Bearer token.

- API keys are created in the UI
- requests must send: `Authorization: Bearer <token>`

## Model naming

The router resolves model names in the form:

- `provider.model`

Example:

- `ollama.llama3.1:8b`
- `vllm.Qwen3-8B`

You can also create aliases such as:

- alias: `my-fast-model`
- actual: `vllm.Qwen3-8B`

Aliases are resolved before provider selection.

## Web UI

The UI includes:

- API key management
- provider list and health checks
- model alias management
- live in-flight request monitoring

## Example configuration

Providers are created through the UI, but the underlying config structure is simple:

```json
{
  "id": "ollama",
  "type": "OLLAMA",
  "url": "http://localhost:11434",
  "resourcePool": "gpu-a"
}
```

```json
{
  "id": "vllm",
  "type": "VLLM",
  "url": "http://localhost:8000",
  "resourcePool": "gpu-a"
}
```

```json
{
  "id": "openai",
  "type": "OPENAI",
  "url": "https://api.openai.com",
  "apiKey": "sk-...",
  "resourcePool": "cloud"
}
```

The resourcePool is what decides whether a provider participates in local sleeping behavior. Cloud providers are not locked and are treated as external resources.

## Request examples

OpenAI-style request through the router:

```bash
curl -X POST http://localhost:8080/inference/v1/chat/completions \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "ollama.llama3.1:8b",
    "messages": [
      {"role": "user", "content": "Hello from Local Router"}
    ]
  }'
```

Direct provider proxy route:

```bash
curl -X POST http://localhost:8080/proxy/ollama/api/generate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama3.1:8b",
    "prompt": "Say hello",
    "stream": false
  }'
```

## Notes

This project is oriented toward local GPU orchestration rather than being a general-purpose reverse proxy. It is designed for the specific case where you want one OpenAI-compatible API facade over a set of local inference backends while preventing them from trampling each other in the same VRAM pool.

## License

This project is licensed under the MIT license. See LICENSE for details.
