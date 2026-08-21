<script lang="ts">
	import type { Model } from '$lib/api';
	import * as API from '$lib/api';

	interface Props {
		models: Model[];
		onUpdate?: () => void;
	}

	let { models, onUpdate }: Props = $props();

	let alias = $state('');
	let actual = $state('');

	let sendButtonDisabled = $derived(alias.length == 0 || actual.length == 0);

	$effect(() => {
		if ((actual == '' || !models.some((model) => model.id == actual)) && models.length > 0) {
			actual = models[0].id;
		}
	});
</script>

<form autocomplete="off" onsubmit={() => {}} class="mt-6 flex items-center space-x-2">
	<input
		bind:value={alias}
		type="text"
		placeholder="Alias..."
		class="h-8 w-full flex-1 rounded-lg border border-sand-4 bg-sand-2 px-2 py-1 text-xs text-sand-12 hover:bg-sand-3 focus:ring-2 focus:ring-amber-7 focus:outline-none"
	/>

	<select
		class="h-8 w-full flex-1 rounded-lg border border-sand-4 bg-sand-2 px-2 py-1 text-xs text-sand-12 hover:bg-sand-3 focus:ring-2 focus:ring-amber-7 focus:outline-none"
		bind:value={actual}
	>
		{#each models as model}
			<option value={model.id}>{model.id}</option>
		{/each}
	</select>

	<button
		type="button"
		class="focus-ring-2 rounded-lg bg-sand-3 p-2 text-sand-12 hover:bg-sand-4 focus:ring-amber-7 focus:outline-none"
		onclick={() => {
			prompt('Copy model name to clipboard', actual);
		}}
		title="Copy Model Name"
	>
		<span class="sr-only">Copy Model Name</span>

		<svg
			xmlns="http://www.w3.org/2000/svg"
			class="h-4 w-4"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="2"
			stroke-linecap="round"
			stroke-linejoin="round"
			><rect width="14" height="14" x="8" y="8" rx="2" ry="2" /><path
				d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"
			/></svg
		>
	</button>

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

			await API.createModelAlias({ alias, actual });

			alias = '';
			actual = '';
			onUpdate?.();
		}}
	>
		<span class="sr-only">Create Model Alias</span>

		<svg
			xmlns="http://www.w3.org/2000/svg"
			viewBox="0 0 24 24"
			class="h-4 w-4"
			fill="none"
			stroke="currentColor"
			stroke-width="2"
			stroke-linecap="round"
			stroke-linejoin="round"
			><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" /><path
				d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"
			/></svg
		>
	</button>
</form>
