use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct ListItemsResult<T> {
    pub items: Vec<T>,
    pub total: i32,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct ListPostResult {
    pub id: i32,
    pub name: String,
    pub short_text: String,
    pub created_at: String,
    pub tags: Vec<TagResult>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct PostResult {
    pub name: String,
    pub text: String,
    pub created_at: String,
    pub tags: Vec<TagResult>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct TagResult {
    pub id: i32,
    pub name: String,
    pub slug: String,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct TagWithCountResult {
    pub id: i32,
    pub name: String,
    pub slug: String,
    pub post_count: i32,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct TagCloudItem {
    pub name: String,
    pub slug: String,
    pub count: i32,
    pub weight: f64,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct TagCloudResult {
    pub tags: Vec<TagCloudItem>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct CommentResult {
    pub id: i32,
    pub name: String,
    pub text: String,
    pub rating: i32,
    pub created_at: String,
    pub replies: Vec<CommentResult>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct CommentsListResult {
    pub comments: Vec<CommentResult>,
    pub total: i32,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct PageResult {
    pub id: i32,
    pub url: String,
    pub title: String,
    pub content: String,
    pub created_at: String,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct ListPageResult {
    pub url: String,
    pub title: String,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct CreateCommentRequest {
    pub name: String,
    pub email: String,
    pub text: String,
    pub parent_id: Option<i32>,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct RateCommentRequest {
    pub is_upvote: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct ErrorResponse {
    pub code: String,
    pub message: String,
    pub details: Option<std::collections::HashMap<String, String>>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct HealthResponse {
    pub status: String,
    pub database: String,
    pub timestamp: String,
}

// --- Skills ---

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct SkillResult {
    pub id: i32,
    pub name: String,
    pub slug: String,
    pub category: String,
    pub proficiency: i32,
    pub icon: Option<String>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct SkillCategoryResult {
    pub category: String,
    pub skills: Vec<SkillResult>,
}

// --- Experiences ---

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct ExperienceResult {
    pub id: i32,
    pub company: String,
    pub position: String,
    pub description: String,
    pub start_date: String,
    pub end_date: Option<String>,
    pub location: Option<String>,
    pub company_url: Option<String>,
}

// --- Social Links ---

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct SocialLinkResult {
    pub id: i32,
    pub platform: String,
    pub url: String,
    pub label: Option<String>,
    pub icon: Option<String>,
}

// --- Contact ---

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct CreateContactRequest {
    pub name: String,
    pub email: String,
    pub subject: String,
    pub message: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub website: Option<String>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct ContactResponse {
    pub message: String,
}

// --- About (aggregated) ---

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct ProfileResult {
    pub name: String,
    pub title: String,
    pub photo_url: String,
    pub resume_url: String,
    pub bio: String,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct AboutResult {
    pub profile: ProfileResult,
    pub skills: Vec<SkillCategoryResult>,
    pub experiences: Vec<ExperienceResult>,
    pub social_links: Vec<SocialLinkResult>,
}
