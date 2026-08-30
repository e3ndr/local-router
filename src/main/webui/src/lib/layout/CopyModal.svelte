<script lang="ts">
	import Modal from './Modal.svelte';

	interface Props {
		title: string;
		message: string;
		value: string;
		onClose: () => void;
	}

	let { title, message, value, onClose }: Props = $props();

	let copied = $state(false);

	async function copy() {
		try {
			if (navigator.clipboard && window.isSecureContext) {
				await navigator.clipboard.writeText(value);
			} else {
				// Non-secure context (plain http over a LAN IP) - fall back to a legacy copy.
				const el = document.createElement('textarea');
				el.value = value;
				el.style.position = 'fixed';
				el.style.opacity = '0';
				document.body.appendChild(el);
				el.select();
				document.execCommand('copy');
				document.body.removeChild(el);
			}
			copied = true;
		} catch {
			copied = false;
		}
	}
</script>

<Modal {title} {onClose}>
	<p class="text-xs text-sand-11">{message}</p>

	<input
		readonly
		type="text"
		value={value}
		class="h-8 w-full rounded-lg border border-sand-4 bg-sand-3 px-2 py-1 font-mono text-xs text-sand-12 focus:ring-2 focus:ring-amber-7 focus:outline-none"
		onclick={(e) => (e.target as HTMLInputElement).select()}
	/>

	<div class="flex justify-end space-x-2">
		<button
			type="button"
			onclick={copy}
			class="rounded-lg bg-sand-3 px-3 py-1.5 text-xs text-sand-12 hover:bg-sand-4 focus:ring-2 focus:ring-amber-7 focus:outline-none"
		>
			{copied ? 'Copied' : 'Copy'}
		</button>

		<button
			type="button"
			onclick={onClose}
			class="rounded-lg bg-sand-3 px-3 py-1.5 text-xs text-sand-12 hover:bg-sand-4 focus:ring-2 focus:ring-amber-7 focus:outline-none"
		>
			Close
		</button>
	</div>
</Modal>
