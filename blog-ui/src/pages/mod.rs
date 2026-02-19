pub mod home;
pub mod not_found;
pub mod post_detail;
pub mod post_list;
pub mod search;
pub mod static_page;
pub mod tag_list;
pub mod tag_posts;

pub use home::HomePage;
pub use not_found::NotFoundPage;
pub use post_detail::PostDetailPage;
pub use post_list::PostListPage;
pub use search::SearchPage;
pub use static_page::StaticPageView;
pub use tag_list::TagListPage;
pub use tag_posts::TagPostsPage;
