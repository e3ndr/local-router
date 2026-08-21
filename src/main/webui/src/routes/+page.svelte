<script lang="ts">
	import type { Model, ApiKey, ModelAlias, InferenceProvider, InFlightInference } from '$lib/api';
	import * as API from '$lib/api';
	import ApiKeyCard from '$lib/layout/ApiKeyCard.svelte';
	import ApiKeyCreateCard from '$lib/layout/ApiKeyCreateCard.svelte';
	import InferenceProviderCard from '$lib/layout/InferenceProviderCard.svelte';
	import InferenceProviderCreateCard from '$lib/layout/InferenceProviderCreateCard.svelte';
	import InferenceProviderLogo from '$lib/layout/InferenceProviderLogo.svelte';
	import LoadingSpinner from '$lib/layout/LoadingSpinner.svelte';
	import ModelAliasCard from '$lib/layout/ModelAliasCard.svelte';
	import ModelAliasCreateCard from '$lib/layout/ModelAliasCreateCard.svelte';
	import { onMount } from 'svelte';

	let apiKeyRerender = $state(0);
	let modelAliasRerender = $state(0);
	let providerRerender = $state(0);

	let apiKeys: ApiKey[] = $state([]);
	let modelAliases: ModelAlias[] = $state([]);
	let models: Model[] = $state([]);
	let providers: InferenceProvider[] = $state([]);

	let providerHealth: Record<string, boolean> = $state({});
	let providersById: Record<string, InferenceProvider> = $state({});
	let inFlight: InFlightInference[] = $state([]);

	function updateProviderHealth() {
		API.getProviderHealth().then((health) => (providerHealth = health));
	}

	function updateInFlight() {
		API.getInFlight().then((i) => (inFlight = i));
	}

	$effect(() => {
		apiKeyRerender; // track.

		API.listApiKeys().then((fetchedKeys) => {
			apiKeys = fetchedKeys;
		});
	});

	$effect(() => {
		modelAliasRerender; // track.

		API.listModelAliases().then((fetchedAliases) => {
			modelAliases = fetchedAliases;
		});
	});

	$effect(() => {
		providerRerender; // track.

		API.listModels().then((fetchedModels) => {
			models = fetchedModels;
		});

		API.listProviders().then((fetchedProviders) => {
			providers = fetchedProviders;
			providersById = Object.fromEntries(fetchedProviders.map((p) => [p.id, p]));
		});

		updateProviderHealth();
	});

	onMount(() => {
		updateProviderHealth();
		const interval = setInterval(updateProviderHealth, 10000); // Update every 10 seconds
		return () => clearInterval(interval);
	});

	onMount(() => {
		updateInFlight();
		const interval = setInterval(updateInFlight, 1000); // Update every 1 second
		return () => clearInterval(interval);
	});
</script>

<main class="mx-auto max-w-lg space-y-12 p-4">
	<div>
		<h2 class="mb-2 text-xl">API Keys</h2>
		{#if apiKeys.length > 0}
			<ul class="flex flex-col gap-2">
				{#each apiKeys as key}
					<li>
						<ApiKeyCard {key} onUpdate={() => apiKeyRerender++} />
					</li>
				{/each}
			</ul>
		{:else}
			<p class="text-sm text-sand-11">No API keys found.</p>
		{/if}

		<ApiKeyCreateCard onUpdate={() => apiKeyRerender++} />
	</div>

	<div>
		<h2 class="mb-2 text-xl">Model Aliases</h2>
		{#if modelAliases.length > 0}
			<ul class="flex flex-col gap-2">
				{#each modelAliases as alias}
					<li>
						<ModelAliasCard {alias} onUpdate={() => modelAliasRerender++} />
					</li>
				{/each}
			</ul>
		{:else}
			<p class="text-sm text-sand-11">No model aliases found.</p>
		{/if}

		<ModelAliasCreateCard {models} onUpdate={() => modelAliasRerender++} />
	</div>

	<div>
		<h2 class="mb-2 text-xl">Model Providers</h2>
		{#if providers.length > 0}
			<ul class="flex flex-col gap-2">
				{#each providers as provider}
					<li>
						<InferenceProviderCard
							{provider}
							healthy={providerHealth[provider.id] ?? false}
							onUpdate={() => providerRerender++}
						/>
					</li>
				{/each}
			</ul>
		{:else}
			<p class="text-sm text-sand-11">No providers found.</p>
		{/if}

		<InferenceProviderCreateCard onUpdate={() => providerRerender++} />
	</div>

	<div>
		<h2 class="mb-2 text-xl">Requests In-Flight</h2>
		{#if inFlight.length > 0}
			<ul class="flex flex-col gap-2">
				{#each inFlight as flight}
					<li
						class="flex items-center gap-2 text-sm"
						class:text-sand-11={flight.status == 'WAITING'}
						class:text-sand-12={flight.status == 'RUNNING'}
						class:text-[#e52a2a]={flight.status == 'CANCELLED'}
					>
						<button
							onclick={() => {
								flight.status = 'CANCELLED';
								API.cancelInFlight(flight.id);
							}}
							class="group flex items-center"
						>
							<span class="group-hover:hidden">
								{#if flight.status == 'WAITING'}
									<svg
										xmlns="http://www.w3.org/2000/svg"
										width="14"
										height="14"
										viewBox="0 0 24 24"
										fill="none"
										stroke="currentColor"
										stroke-width="2"
										stroke-linecap="round"
										stroke-linejoin="round"
										><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /><circle
											cx="5"
											cy="12"
											r="1"
										/></svg
									>
								{:else if flight.status == 'CANCELLED'}
									<svg
										xmlns="http://www.w3.org/2000/svg"
										width="14"
										height="14"
										viewBox="0 0 24 24"
										fill="none"
										stroke="currentColor"
										stroke-width="2"
										stroke-linecap="round"
										stroke-linejoin="round"><path d="M18 6 6 18" /><path d="m6 6 12 12" /></svg
									>
								{:else}
									<LoadingSpinner />
								{/if}
							</span>

							<span class="hidden group-hover:block">
								<svg
									xmlns="http://www.w3.org/2000/svg"
									width="14"
									height="14"
									viewBox="0 0 24 24"
									fill="none"
									stroke="currentColor"
									stroke-width="2"
									stroke-linecap="round"
									stroke-linejoin="round"><path d="M18 6 6 18" /><path d="m6 6 12 12" /></svg
								>
							</span>
						</button>

						<InferenceProviderLogo type={providersById[flight.providerId].type} />
						<span>{flight.providerId}</span>
						<span class="text-sand-11">&bull;</span>
						<span class="text-sand-11">{flight.modelId}</span>
					</li>
				{/each}
			</ul>
		{:else}
			<p class="text-sm text-sand-11">No requests in-flight.</p>
		{/if}
	</div>
</main>
