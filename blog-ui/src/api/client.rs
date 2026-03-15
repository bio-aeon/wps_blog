use super::types::*;

#[derive(Debug, thiserror::Error)]
pub enum ApiError {
    #[error("Network error: {0}")]
    Network(#[from] reqwest::Error),
    #[error("Not found: {0}")]
    NotFound(String),
    #[error("Validation error: {0}")]
    Validation(String),
    #[error("Server error: {0}")]
    Server(String),
}

pub struct BlogApiClient {
    base_url: String,
    client: reqwest::Client,
}

impl BlogApiClient {
    pub fn new(base_url: String) -> Self {
        Self {
            base_url,
            client: reqwest::Client::new(),
        }
    }

    fn url(&self, path: &str) -> String {
        format!("{}{}", self.base_url, path)
    }

    async fn handle_response<T: serde::de::DeserializeOwned>(
        &self,
        resp: reqwest::Response,
    ) -> Result<T, ApiError> {
        let status = resp.status();
        if status.is_success() {
            Ok(resp.json::<T>().await?)
        } else if status.as_u16() == 404 {
            let body = resp.text().await.unwrap_or_default();
            Err(ApiError::NotFound(body))
        } else if status.as_u16() == 422 {
            let body = resp.text().await.unwrap_or_default();
            Err(ApiError::Validation(body))
        } else {
            let body = resp.text().await.unwrap_or_default();
            Err(ApiError::Server(body))
        }
    }

    pub async fn get_posts(
        &self,
        limit: i32,
        offset: i32,
        tag: Option<&str>,
    ) -> Result<ListItemsResult<ListPostResult>, ApiError> {
        let mut url = format!("{}/v1/posts?limit={}&offset={}", self.base_url, limit, offset);
        if let Some(tag) = tag {
            url.push_str(&format!("&tag={}", urlencoding::encode(tag)));
        }
        let resp = self.client.get(url).send().await?;
        self.handle_response(resp).await
    }

    pub async fn search_posts(
        &self,
        query: &str,
        limit: i32,
        offset: i32,
    ) -> Result<ListItemsResult<ListPostResult>, ApiError> {
        let url = format!(
            "{}/v1/posts/search?q={}&limit={}&offset={}",
            self.base_url,
            urlencoding::encode(query),
            limit,
            offset
        );
        let resp = self.client.get(url).send().await?;
        self.handle_response(resp).await
    }

    pub async fn get_recent_posts(
        &self,
        count: i32,
    ) -> Result<Vec<ListPostResult>, ApiError> {
        let url = format!("{}/v1/posts/recent?count={}", self.base_url, count);
        let resp = self.client.get(url).send().await?;
        self.handle_response(resp).await
    }

    pub async fn get_post(&self, id: i32) -> Result<PostResult, ApiError> {
        let resp = self
            .client
            .get(self.url(&format!("/v1/posts/{}", id)))
            .send()
            .await?;
        self.handle_response(resp).await
    }

    pub async fn increment_view(&self, id: i32) -> Result<(), ApiError> {
        let resp = self
            .client
            .post(self.url(&format!("/v1/posts/{}/view", id)))
            .send()
            .await?;
        if resp.status().is_success() {
            Ok(())
        } else {
            let body = resp.text().await.unwrap_or_default();
            Err(ApiError::Server(body))
        }
    }

    pub async fn get_comments(
        &self,
        post_id: i32,
    ) -> Result<CommentsListResult, ApiError> {
        let resp = self
            .client
            .get(self.url(&format!("/v1/posts/{}/comments", post_id)))
            .send()
            .await?;
        self.handle_response(resp).await
    }

    pub async fn create_comment(
        &self,
        post_id: i32,
        req: CreateCommentRequest,
    ) -> Result<CommentResult, ApiError> {
        let resp = self
            .client
            .post(self.url(&format!("/v1/posts/{}/comments", post_id)))
            .json(&req)
            .send()
            .await?;
        self.handle_response(resp).await
    }

    pub async fn rate_comment(
        &self,
        comment_id: i32,
        is_upvote: bool,
    ) -> Result<(), ApiError> {
        let resp = self
            .client
            .post(self.url(&format!("/v1/comments/{}/rate", comment_id)))
            .json(&RateCommentRequest { is_upvote })
            .send()
            .await?;
        if resp.status().is_success() {
            Ok(())
        } else {
            let body = resp.text().await.unwrap_or_default();
            Err(ApiError::Server(body))
        }
    }

    pub async fn get_tags(&self) -> Result<ListItemsResult<TagWithCountResult>, ApiError> {
        let resp = self.client.get(self.url("/v1/tags")).send().await?;
        self.handle_response(resp).await
    }

    pub async fn get_tag_cloud(&self) -> Result<TagCloudResult, ApiError> {
        let resp = self.client.get(self.url("/v1/tags/cloud")).send().await?;
        self.handle_response(resp).await
    }

    pub async fn get_pages(&self) -> Result<ListItemsResult<ListPageResult>, ApiError> {
        let resp = self.client.get(self.url("/v1/pages")).send().await?;
        self.handle_response(resp).await
    }

    pub async fn get_page(&self, url: &str) -> Result<PageResult, ApiError> {
        let resp = self
            .client
            .get(self.url(&format!("/v1/pages/{}", url)))
            .send()
            .await?;
        self.handle_response(resp).await
    }

    pub async fn get_skills(&self) -> Result<Vec<SkillCategoryResult>, ApiError> {
        let resp = self.client.get(self.url("/v1/skills")).send().await?;
        self.handle_response(resp).await
    }

    pub async fn get_experiences(&self) -> Result<Vec<ExperienceResult>, ApiError> {
        let resp = self
            .client
            .get(self.url("/v1/experiences"))
            .send()
            .await?;
        self.handle_response(resp).await
    }

    pub async fn get_social_links(&self) -> Result<Vec<SocialLinkResult>, ApiError> {
        let resp = self
            .client
            .get(self.url("/v1/social-links"))
            .send()
            .await?;
        self.handle_response(resp).await
    }

    pub async fn submit_contact(
        &self,
        request: &CreateContactRequest,
    ) -> Result<ContactResponse, ApiError> {
        let resp = self
            .client
            .post(self.url("/v1/contact"))
            .json(request)
            .send()
            .await?;
        self.handle_response(resp).await
    }

    pub async fn get_about(&self) -> Result<AboutResult, ApiError> {
        let resp = self.client.get(self.url("/v1/about")).send().await?;
        self.handle_response(resp).await
    }
}
