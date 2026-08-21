<script lang="ts">
	import * as API from '$lib/api';
	import type { InferenceProvider } from '$lib/api';
	import InferenceProviderLogo from './InferenceProviderLogo.svelte';

	interface Props {
		provider: InferenceProvider;
		healthy: boolean;
		onUpdate?: () => void;
	}

	let { provider, healthy, onUpdate }: Props = $props();
</script>

<div class="flex items-center gap-2 rounded-lg border border-sand-6 bg-sand-2 p-2">
	<div
		class="h-1.5 w-1.5 rounded-full"
		class:bg-[#2ae52f]={healthy}
		class:bg-[#e52a2a]={!healthy}
	></div>

	<div class="flex flex-1 items-center">
		<InferenceProviderLogo type={provider.type} />

		<span class="ml-1 font-mono">{provider.id}</span>

		{#if provider.resourcePool != 'cloud'}
			<span class="text-sm text-sand-11"> &bull; {provider.resourcePool} </span>
		{/if}
	</div>

	<button
		onclick={async () => {
			await API.deleteProvider(provider.id);
			onUpdate?.();
		}}
		class="float-right rounded-lg p-1 text-sm text-sand-11 hover:bg-sand-4 hover:text-sand-12 focus:ring-2 focus:ring-amber-7 focus:outline-none active:bg-sand-4"
	>
		<span class="sr-only">Remove Inference Provider</span>

		<svg
			xmlns="http://www.w3.org/2000/svg"
			class="h-4 w-4"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="2"
			stroke-linecap="round"
			stroke-linejoin="round"
			><path d="M10 11v6" /><path d="M14 11v6" /><path
				d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"
			/><path d="M3 6h18" /><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg
		>
	</button>
</div>
