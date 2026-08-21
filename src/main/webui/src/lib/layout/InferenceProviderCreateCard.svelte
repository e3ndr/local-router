<script lang="ts">
	import type { InferenceProviderType } from '$lib/api';
	import * as API from '$lib/api';

	interface Props {
		onUpdate?: () => void;
	}

	let { onUpdate }: Props = $props();

	const REQUIRES_RESOURCE_POOL: InferenceProviderType[] = ['OLLAMA', 'VLLM'];
	const REQUIRES_URL: InferenceProviderType[] = ['OLLAMA', 'VLLM', 'GENERIC'];

	let type: InferenceProviderType = $state('OLLAMA');
	let id = $state('');
	let resourcePool = $state('');
	let url = $state('');
	let apiKey = $state('');

	let sendButtonDisabled = $derived(
		id.length == 0 ||
			(REQUIRES_RESOURCE_POOL.includes(type) && resourcePool.length == 0) ||
			(REQUIRES_URL.includes(type) && url.length == 0)
	);
</script>

<form autocomplete="off" onsubmit={() => {}} class="mt-6 space-y-2">
	<div class="flex items-center space-x-2">
		<select
			class="h-8 w-full flex-1 rounded-lg border border-sand-4 bg-sand-2 px-2 py-1 text-xs text-sand-12 hover:bg-sand-3 focus:ring-2 focus:ring-amber-7 focus:outline-none"
			bind:value={type}
		>
			<option value="OLLAMA">Ollama</option>
			<option value="VLLM">vLLM</option>
			<option value="GENERIC">Generic</option>
			<option value="OPENAI">OpenAI</option>
			<option value="DEEPINFRA">DeepInfra</option>
			<option value="GROQ">GROQ</option>
			<option value="TOGETHER_AI">Together AI</option>
			<option value="FIREWORKS_AI">Fireworks AI</option>
			<option value="CEREBRAS">Cerebras</option>
			<option value="DEEPSEEK">DeepSeek</option>
			<option value="MISTRAL">Mistral</option>
			<option value="XAI">XAI</option>
			<option value="OPENROUTER">OpenRouter</option>
			<option value="PERPLEXITY">Perplexity</option>
			<option value="SAMBANOVA">SambaNova</option>
			<option value="NVIDIA_NIM">NVIDIA NIM</option>
			<option value="NOVITA">Novita</option>
			<option value="QWEN">Qwen</option>
			<option value="MOONSHOT">Moonshot</option>
			<option value="GOOGLE_GEMINI">Google Gemini</option>
			<option value="CHUTES">Chutes</option>
			<option value="COHERE">Cohere</option>
			<option value="HUGGINGFACE">HuggingFace</option>
			<option value="POOLSIDE">Poolside</option>
			<option value="BASETEN">BaseTen</option>
		</select>

		<input
			bind:value={id}
			type="text"
			placeholder="ID..."
			class="h-8 w-16 rounded-lg border border-sand-4 bg-sand-2 px-2 py-1 text-xs text-sand-12 hover:bg-sand-3 focus:ring-2 focus:ring-amber-7 focus:outline-none"
			class:flex-1={!REQUIRES_RESOURCE_POOL.includes(type)}
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
			type="text"
			placeholder="API Key (optional)..."
			class="h-8 w-full flex-1 rounded-lg border border-sand-4 bg-sand-2 px-2 py-1 text-xs text-sand-12 hover:bg-sand-3 focus:ring-2 focus:ring-amber-7 focus:outline-none"
		/>

		<button
			type="submit"
			disabled={sendButtonDisabled}
			class:text-sand-11={sendButtonDisabled}
			class:text-sand-12={!sendButtonDisabled}
			class:focus-ring-2={!sendButtonDisabled}
			class:hover:bg-sand-4={!sendButtonDisabled}
			class="rounded-lg bg-sand-3 p-2 focus:ring-amber-7 focus:outline-none"
			onclick={async () => {
				if (sendButtonDisabled) return;

				await API.createProvider({
					type,
					id,
					resourcePool: type == 'OPENAI' ? 'cloud' : resourcePool,
					url,
					apiKey
				});

				id = '';
				resourcePool = 'local';
				url = '';
				apiKey = '';
				onUpdate?.();
			}}
		>
			<span class="sr-only">Add Inference Provider</span>

			<svg
				xmlns="http://www.w3.org/2000/svg"
				class="h-4 w-4"
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="2"
				stroke-linecap="round"
				stroke-linejoin="round"><path d="M5 12h14" /><path d="M12 5v14" /></svg
			>
		</button>
	</div>
</form>
