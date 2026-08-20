<script lang="ts">
	import * as API from '$lib/api';
	import type { ApiKey } from '$lib/api';

	interface Props {
		key: ApiKey;
		onUpdate?: () => void;
	}

	let { key, onUpdate }: Props = $props();

	// svelte-ignore state_referenced_locally
	let descriptionEdit = $state(key.description);
</script>

<div class="flex items-center rounded-lg border border-sand-6 bg-sand-2">
	<input
		bind:value={descriptionEdit}
		onchange={async () => {
			await API.editApiKey(key.id, descriptionEdit);
			key.description = descriptionEdit;
		}}
		type="text"
		placeholder="Description..."
		class="ml-0.5 w-full flex-1 rounded-sm border-none px-2 py-2 text-xs text-sand-12 hover:bg-sand-4 focus:ring-2 focus:ring-amber-7 focus:outline-none"
	/>

	<button
		onclick={async () => {
			await API.deleteApiKey(key.id);
			onUpdate?.();
		}}
		class="float-right m-2 rounded-lg p-1 text-sm text-sand-11 hover:bg-sand-4 hover:text-sand-12 focus:ring-2 focus:ring-amber-7 focus:outline-none active:bg-sand-4"
	>
		<span class="sr-only">Revoke API Key</span>

		<svg
			xmlns="http://www.w3.org/2000/svg"
			class="h-4 w-4"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="2"
			stroke-linecap="round"
			stroke-linejoin="round"
			><path d="m15 9-6 6" /><path
				d="M2.586 16.726A2 2 0 0 1 2 15.312V8.688a2 2 0 0 1 .586-1.414l4.688-4.688A2 2 0 0 1 8.688 2h6.624a2 2 0 0 1 1.414.586l4.688 4.688A2 2 0 0 1 22 8.688v6.624a2 2 0 0 1-.586 1.414l-4.688 4.688a2 2 0 0 1-1.414.586H8.688a2 2 0 0 1-1.414-.586z"
			/><path d="m9 9 6 6" /></svg
		>
	</button>
</div>
