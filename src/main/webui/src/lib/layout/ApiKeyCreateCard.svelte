<script lang="ts">
	import * as API from '$lib/api';

	interface Props {
		onUpdate?: () => void;
	}

	let { onUpdate }: Props = $props();

	let description = $state('');

	let sendButtonDisabled = $derived(description.length == 0);
</script>

<form autocomplete="off" onsubmit={() => {}} class="mt-6 flex items-center space-x-2">
	<input
		bind:value={description}
		type="text"
		placeholder="Description..."
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

			const created = await API.createApiKey(description);

			prompt(
				'API Key created.\n\nPlease copy it now, as it will not be shown again.\n',
				created.key as string
			);

			description = '';
			onUpdate?.();
		}}
	>
		<span class="sr-only">Issue a new API Key</span>

		<svg
			xmlns="http://www.w3.org/2000/svg"
			class="h-4 w-4"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="2"
			stroke-linecap="round"
			stroke-linejoin="round"
			><path
				d="M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z"
			/><circle cx="16.5" cy="7.5" r=".5" fill="currentColor" /></svg
		>
	</button>
</form>
