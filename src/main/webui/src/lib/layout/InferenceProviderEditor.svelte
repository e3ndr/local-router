<script lang="ts">
	import * as API from '$lib/api';
	import type { InferenceProvider, InferenceProviderType } from '$lib/api';
	import { API_KEY_SENTINEL } from '$lib/api';
	import Modal from './Modal.svelte';

	interface Props {
		provider: InferenceProvider;
		onClose: () => void;
		onSaved?: () => void;
	}

	let { provider, onClose, onSaved }: Props = $props();

	const REQUIRES_RESOURCE_POOL: InferenceProviderType[] = ['OLLAMA', 'VLLM'];
	const REQUIRES_URL: InferenceProviderType[] = ['OLLAMA', 'VLLM', 'GENERIC'];

	let type: InferenceProviderType = $state('OLLAMA');
	let id = $state('');
	let resourcePool = $state('');
	let url = $state('');
	let apiKey = $state('');
	let concurrency = $state('1');
	let concurrencyMatchesModel = $state(false);

	$effect(() => {
		// Prefill the form from the provider when the editor opens.
		type = provider.type;
		id = provider.id;
		resourcePool = provider.resourcePool ?? '';
		url = provider.url ?? '';
		apiKey = provider.apiKey ?? '';
		concurrency = String(provider.concurrency ?? 1);
		concurrencyMatchesModel = provider.concurrencyMatchesModel ?? false;
	});

	let saving = $state(false);
	let error = $state('');

	let sendButtonDisabled = $derived(
		saving ||
			(REQUIRES_RESOURCE_POOL.includes(type) && resourcePool.length == 0) ||
			(REQUIRES_URL.includes(type) && url.length == 0)
	);

	const TYPE_OPTIONS: [InferenceProviderType, string][] = [
		['OLLAMA', 'Ollama'],
		['VLLM', 'vLLM'],
		['GENERIC', 'Generic'],
		['OPENAI', 'OpenAI'],
		['DEEPINFRA', 'DeepInfra'],
		['GROQ', 'GROQ'],
		['TOGETHER_AI', 'Together AI'],
		['FIREWORKS_AI', 'Fireworks AI'],
		['CEREBRAS', 'Cerebras'],
		['DEEPSEEK', 'DeepSeek'],
		['MISTRAL', 'Mistral'],
		['XAI', 'XAI'],
		['OPENROUTER', 'OpenRouter'],
		['PERPLEXITY', 'Perplexity'],
		['SAMBANOVA', 'SambaNova'],
		['NVIDIA_NIM', 'NVIDIA NIM'],
		['NOVITA', 'Novita'],
		['QWEN', 'Qwen'],
		['MOONSHOT', 'Moonshot'],
		['GOOGLE_GEMINI', 'Google Gemini'],
		['CHUTES', 'Chutes'],
		['COHERE', 'Cohere'],
		['HUGGINGFACE', 'HuggingFace'],
		['POOLSIDE', 'Poolside'],
		['BASETEN', 'BaseTen']
	];

	async function save() {
		if (sendButtonDisabled) {
			return;
		}

		saving = true;
		error = '';

		try {
			await API.updateProvider(id, {
				type,
				id,
				resourcePool: type == 'OPENAI' ? 'cloud' : resourcePool,
				url,
				apiKey,
				concurrency: Math.max(1, parseInt(concurrency) || 1),
				concurrencyMatchesModel
			});

			onSaved?.();
		} catch (e) {
			error = String(e);
			saving = false;
		}
	}
</script>

<Modal title="Edit Provider" {onClose}>
	<form autocomplete="off" onsubmit={() => {}} class="space-y-2">
		<div class="flex items-center space-x-2">
			<select
				class="h-8 w-full flex-1 rounded-lg border border-sand-4 bg-sand-2 px-2 py-1 text-xs text-sand-12 hover:bg-sand-3 focus:ring-2 focus:ring-amber-7 focus:outline-none"
				bind:value={type}
			>
				{#each TYPE_OPTIONS as [value, label]}
					<option value={value}>{label}</option>
				{/each}
			</select>

			<input
				value={id}
				readonly
				type="text"
				title="The ID cannot be changed - model aliases and in-flight state reference it."
				class="h-8 w-full flex-1 cursor-not-allowed rounded-lg border border-sand-4 bg-sand-3 px-2 py-1 text-xs text-sand-11 focus:ring-2 focus:ring-amber-7 focus:outline-none"
			/>

			{#if REQUIRES_RESOURCE_POOL.includes(type)}
				<input
					bind:value={resourcePool}
					type="text"
					placeholder="Resource Pool..."
					class="h-8 w-full flex-1 rounded-lg border border-sand-4 bg-sand-2 px-2 py-1 text-xs text-sand-12 hover:bg-sand-3 focus:ring-2 focus:ring-amber-7 focus:outline-none"
				/>
			{/if}
		</div>

		{#if REQUIRES_RESOURCE_POOL.includes(type)}
			<div class="flex items-center space-x-3">
				<input
					bind:value={concurrency}
					type="number"
					min="1"
					placeholder="Concurrency..."
					title="Max parallel requests on this provider (default 1)"
					class="h-8 w-24 rounded-lg border border-sand-4 bg-sand-2 px-2 py-1 text-xs text-sand-12 hover:bg-sand-3 focus:ring-2 focus:ring-amber-7 focus:outline-none"
				/>

				<label class="flex items-center gap-1 text-xs text-sand-12">
					<input
						bind:checked={concurrencyMatchesModel}
						type="checkbox"
						class="rounded border-sand-4"
					/>
					Same-model only
				</label>
			</div>
		{/if}

		<div class="flex items-center space-x-2">
			{#if REQUIRES_URL.includes(type)}
				<input
					bind:value={url}
					type="text"
					placeholder="URL..."
					class="h-8 w-full flex-1 rounded-lg border border-sand-4 bg-sand-2 px-2 py-1 text-xs text-sand-12 hover:bg-sand-3 focus:ring-2 focus:ring-amber-7 focus:outline-none"
				/>
			{/if}

			<input
				bind:value={apiKey}
				type="password"
				placeholder="API Key (optional)..."
				title={apiKey == API_KEY_SENTINEL ? 'A key is set - leave as-is to keep it, or type a new key to replace it.' : 'API key (optional)'}
				class="h-8 w-full flex-1 rounded-lg border border-sand-4 bg-sand-2 px-2 py-1 text-xs text-sand-12 hover:bg-sand-3 focus:ring-2 focus:ring-amber-7 focus:outline-none"
			/>
		</div>

		{#if error.length > 0}
			<p class="text-xs text-[#e52a2a]">{error}</p>
		{/if}

		<div class="flex items-center justify-between gap-2">
			<p class="flex-1 text-xs text-sand-11">The ID is fixed - model aliases reference it.</p>

			<div class="flex space-x-2">
				<button
					type="button"
					onclick={onClose}
					class="rounded-lg bg-sand-3 px-3 py-1.5 text-xs text-sand-12 hover:bg-sand-4 focus:ring-2 focus:ring-amber-7 focus:outline-none"
				>
					Cancel
				</button>

				<button
					type="submit"
					disabled={sendButtonDisabled}
					class:text-sand-11={sendButtonDisabled}
					class:text-sand-12={!sendButtonDisabled}
					class="rounded-lg bg-sand-3 px-3 py-1.5 text-xs focus:ring-amber-7 focus:outline-none"
					class:hover:bg-sand-4={!sendButtonDisabled}
					onclick={save}
				>
					{saving ? 'Saving...' : 'Save'}
				</button>
			</div>
		</div>
	</form>
</Modal>
