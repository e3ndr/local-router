import adapter from '@sveltejs/adapter-static';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	kit: {
		adapter: adapter({
			assets: '../../../target/classes/webui',
			pages: '../../../target/classes/webui',
			fallback: '__fallback.html'
		})
	}
};

export default config;
