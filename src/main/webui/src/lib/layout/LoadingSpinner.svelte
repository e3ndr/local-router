<span class="loader"></span>

<style>
	.loader {
		width: 14px;
		height: 14px;
		border-radius: 50%;
		display: inline-block;
		border-top: 3px solid var(--radix-intermediate-sand-12);
		border-right: 3px solid transparent;
		box-sizing: border-box;
		animation: rotation 1s linear infinite;
	}

	@keyframes rotation {
		0% {
			transform: rotate(0deg);
		}

		100% {
			transform: rotate(360deg);
		}
	}
</style>
