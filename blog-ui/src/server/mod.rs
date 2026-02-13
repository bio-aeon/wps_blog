use std::env;

const DEFAULT_API_URL: &str = "http://localhost:9000";

pub fn get_api_base_url() -> String {
    env::var("BLOG_API_URL").unwrap_or_else(|_| DEFAULT_API_URL.to_string())
}
