<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		title: string;
		onClose: () => void;
		children: Snippet;
	}

	let { title, onClose, children }: Props = $props();
</script>

<div
	class="fixed inset-0 z-40 flex items-center justify-center bg-sand-1/60 p-4"
	role="dialog"
	aria-modal="true"
	tabindex="-1"
	onclick={(e) => e.target === e.currentTarget && onClose()}
	onkeydown={(e) => e.key === 'Escape' && onClose()}
>
	<div class="w-full max-w-md space-y-3 rounded-xl border border-sand-6 bg-sand-2 p-4 shadow-lg">
		<div class="flex items-center justify-between">
			<h3 class="text-sm font-semibold text-sand-12">{title}</h3>

			<button
				type="button"
				onclick={onClose}
				class="rounded-lg p-1 text-sand-11 hover:bg-sand-4 hover:text-sand-12 focus:ring-2 focus:ring-amber-7 focus:outline-none"
			>
				<span class="sr-only">Close</span>

				<svg
					xmlns="http://www.w3.org/2000/svg"
					class="h-4 w-4"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2"
					stroke-linecap="round"
					stroke-linejoin="round"><path d="M18 6 6 18" /><path d="m6 6 12 12" /></svg
				>
			</button>
		</div>

		{@render children()}
	</div>
</div>
