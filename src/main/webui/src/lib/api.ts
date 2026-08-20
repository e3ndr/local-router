import { dev } from '$app/environment';

const ENDPOINT = dev ? 'http://localhost:8081' : ``;

// ----------------------------------------------------------------

export declare type InferenceProviderType = 'OLLAMA' | 'VLLM' | 'OPENAI';

export declare interface InferenceProvider {
	id: string;
	type: InferenceProviderType;
	resourcePool: string;
	healthy: boolean;
}

export declare interface InferenceProviderCreateRequest {
	id: string;
	type: InferenceProviderType;
	resourcePool: string;
	url: string;
	apiKey: string;
}

export declare interface InFlightInference {
	providerId: string;
	modelId: string;
	isWaiting: boolean;
}

export async function listProviders(): Promise<InferenceProvider[]> {
	const response = await fetch(`${ENDPOINT}/api/providers`);
	if (!response.ok) {
		throw await response.text();
	}
	return await response.json();
}

export async function getProviderHealth(): Promise<Record<string, boolean>> {
	const response = await fetch(`${ENDPOINT}/api/providers/health`);
	if (!response.ok) {
		throw await response.text();
	}
	return await response.json();
}

export async function getInFlight(): Promise<InFlightInference[]> {
	const response = await fetch(`${ENDPOINT}/api/providers/inflight`);
	if (!response.ok) {
		throw await response.text();
	}
	return await response.json();
}

export async function createProvider(req: InferenceProviderCreateRequest): Promise<void> {
	const response = await fetch(`${ENDPOINT}/api/providers`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(req)
	});
	if (!response.ok) {
		throw await response.text();
	}
}

export async function deleteProvider(id: string): Promise<void> {
	const response = await fetch(`${ENDPOINT}/api/providers/${id}`, {
		method: 'DELETE'
	});
	if (!response.ok) {
		throw await response.text();
	}
}

// ----------------------------------------------------------------

export declare interface ApiKey {
	key: string | null;
	id: string;
	description: string;
}

export async function listApiKeys(): Promise<ApiKey[]> {
	const response = await fetch(`${ENDPOINT}/api/keys`);
	if (!response.ok) {
		throw await response.text();
	}
	return await response.json();
}

export async function createApiKey(description: string): Promise<ApiKey> {
	const response = await fetch(`${ENDPOINT}/api/keys`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify({ description })
	});
	if (!response.ok) {
		throw await response.text();
	}
	return await response.json();
}

export async function editApiKey(id: string, description: string): Promise<void> {
	const response = await fetch(`${ENDPOINT}/api/keys/${id}`, {
		method: 'PATCH',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify({ description })
	});
	if (!response.ok) {
		throw await response.text();
	}
}

export async function deleteApiKey(id: string): Promise<void> {
	const response = await fetch(`${ENDPOINT}/api/keys/${id}`, {
		method: 'DELETE'
	});
	if (!response.ok) {
		throw await response.text();
	}
}

// ----------------------------------------------------------------

export declare interface ModelAlias {
	alias: string;
	actual: string;
}

export declare interface Model {
	id: string;
}

export async function listModels(): Promise<Model[]> {
	const response = await fetch(`${ENDPOINT}/api/models`);
	if (!response.ok) {
		throw await response.text();
	}
	return await response.json();
}

export async function listModelAliases(): Promise<ModelAlias[]> {
	const response = await fetch(`${ENDPOINT}/api/models/aliases`);
	if (!response.ok) {
		throw await response.text();
	}
	return await response.json();
}

export async function createModelAlias(alias: ModelAlias): Promise<void> {
	const response = await fetch(`${ENDPOINT}/api/models/aliases`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(alias)
	});
	if (!response.ok) {
		throw await response.text();
	}
}

export async function deleteModelAlias(alias: string): Promise<void> {
	const response = await fetch(`${ENDPOINT}/api/models/aliases/${alias}`, {
		method: 'DELETE'
	});
	if (!response.ok) {
		throw await response.text();
	}
}
