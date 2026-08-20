<script lang="ts">
	import * as API from '$lib/api';
	import type { ModelAlias } from '$lib/api';

	interface Props {
		alias: ModelAlias;
		onUpdate?: () => void;
	}

	let { alias, onUpdate }: Props = $props();
</script>

<div class="flex items-center gap-2 rounded-lg border border-sand-6 bg-sand-2 p-2">
	<div class="flex-1 space-x-2">
		<span class="font-mono">{alias.alias}</span>
		<span class="font-mono text-sand-11">→</span>
		<span class="font-mono">{alias.actual}</span>
	</div>

	<button
		onclick={async () => {
			await API.deleteModelAlias(alias.alias);
			onUpdate?.();
		}}
		class="float-right rounded-lg p-1 text-sm text-sand-11 hover:bg-sand-4 hover:text-sand-12 focus:ring-2 focus:ring-amber-7 focus:outline-none active:bg-sand-4"
	>
		<span class="sr-only">Remove Model Alias</span>

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
				d="m18.84 12.25 1.72-1.71h-.02a5.004 5.004 0 0 0-.12-7.07 5.006 5.006 0 0 0-6.95 0l-1.72 1.71"
			/><path
				d="m5.17 11.75-1.71 1.71a5.004 5.004 0 0 0 .12 7.07 5.006 5.006 0 0 0 6.95 0l1.71-1.71"
			/><line x1="8" x2="8" y1="2" y2="5" /><line x1="2" x2="5" y1="8" y2="8" /><line
				x1="16"
				x2="16"
				y1="19"
				y2="22"
			/><line x1="19" x2="22" y1="16" y2="16" /></svg
		>
	</button>
</div>
