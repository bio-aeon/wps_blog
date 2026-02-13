pub mod types;

#[cfg(feature = "ssr")]
pub mod client;

use leptos::prelude::*;
use types::*;

#[cfg(feature = "ssr")]
fn api_client() -> client::BlogApiClient {
    client::BlogApiClient::new(crate::server::get_api_base_url())
}

#[server]
pub async fn get_posts(
    limit: i32,
    offset: i32,
    tag: Option<String>,
) -> Result<ListItemsResult<ListPostResult>, ServerFnError> {
    let client = api_client();
    client
        .get_posts(limit, offset, tag.as_deref())
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn search_posts(
    query: String,
    limit: i32,
    offset: i32,
) -> Result<ListItemsResult<ListPostResult>, ServerFnError> {
    let client = api_client();
    client
        .search_posts(&query, limit, offset)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_recent_posts(count: i32) -> Result<Vec<ListPostResult>, ServerFnError> {
    let client = api_client();
    client
        .get_recent_posts(count)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_post(id: i32) -> Result<PostResult, ServerFnError> {
    let client = api_client();
    client
        .get_post(id)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn increment_view(id: i32) -> Result<(), ServerFnError> {
    let client = api_client();
    client
        .increment_view(id)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_comments(post_id: i32) -> Result<CommentsListResult, ServerFnError> {
    let client = api_client();
    client
        .get_comments(post_id)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn create_comment(
    post_id: i32,
    name: String,
    email: String,
    text: String,
    parent_id: Option<i32>,
) -> Result<CommentResult, ServerFnError> {
    let client = api_client();
    let req = CreateCommentRequest {
        name,
        email,
        text,
        parent_id,
    };
    client
        .create_comment(post_id, req)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn rate_comment(comment_id: i32, is_upvote: bool) -> Result<(), ServerFnError> {
    let client = api_client();
    client
        .rate_comment(comment_id, is_upvote)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_tags() -> Result<ListItemsResult<TagWithCountResult>, ServerFnError> {
    let client = api_client();
    client
        .get_tags()
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_tag_cloud() -> Result<TagCloudResult, ServerFnError> {
    let client = api_client();
    client
        .get_tag_cloud()
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_pages() -> Result<ListItemsResult<ListPageResult>, ServerFnError> {
    let client = api_client();
    client
        .get_pages()
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_page(url: String) -> Result<PageResult, ServerFnError> {
    let client = api_client();
    client
        .get_page(&url)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}
