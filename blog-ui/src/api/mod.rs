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
    lang: String,
    limit: i32,
    offset: i32,
    tag: Option<String>,
) -> Result<ListItemsResult<ListPostResult>, ServerFnError> {
    let client = api_client();
    client
        .get_posts(&lang, limit, offset, tag.as_deref())
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn search_posts(
    lang: String,
    query: String,
    limit: i32,
    offset: i32,
) -> Result<ListItemsResult<ListPostResult>, ServerFnError> {
    let client = api_client();
    client
        .search_posts(&lang, &query, limit, offset)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_recent_posts(lang: String, count: i32) -> Result<Vec<ListPostResult>, ServerFnError> {
    let client = api_client();
    client
        .get_recent_posts(&lang, count)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_post(lang: String, id: i32) -> Result<PostResult, ServerFnError> {
    let client = api_client();
    client
        .get_post(&lang, id)
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
pub async fn get_tags(lang: String) -> Result<ListItemsResult<TagWithCountResult>, ServerFnError> {
    let client = api_client();
    client
        .get_tags(&lang)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_tag_cloud(lang: String) -> Result<TagCloudResult, ServerFnError> {
    let client = api_client();
    client
        .get_tag_cloud(&lang)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_pages(lang: String) -> Result<ListItemsResult<ListPageResult>, ServerFnError> {
    let client = api_client();
    client
        .get_pages(&lang)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_page(lang: String, url: String) -> Result<PageResult, ServerFnError> {
    let client = api_client();
    client
        .get_page(&lang, &url)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_about() -> Result<AboutResult, ServerFnError> {
    let client = api_client();
    client
        .get_about()
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_skills() -> Result<Vec<SkillCategoryResult>, ServerFnError> {
    let client = api_client();
    client
        .get_skills()
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_experiences() -> Result<Vec<ExperienceResult>, ServerFnError> {
    let client = api_client();
    client
        .get_experiences()
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_social_links() -> Result<Vec<SocialLinkResult>, ServerFnError> {
    let client = api_client();
    client
        .get_social_links()
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn get_languages() -> Result<Vec<LanguageInfo>, ServerFnError> {
    let client = api_client();
    client
        .get_languages()
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}

#[server]
pub async fn submit_contact(
    name: String,
    email: String,
    subject: String,
    message: String,
) -> Result<ContactResponse, ServerFnError> {
    let client = api_client();
    let request = CreateContactRequest {
        name,
        email,
        subject,
        message,
        website: None,
    };
    client
        .submit_contact(&request)
        .await
        .map_err(|e| ServerFnError::new(e.to_string()))
}
