use leptos::prelude::*;

#[component]
pub fn PostCardSkeleton() -> impl IntoView {
    view! {
        <div class="post-card-skeleton">
            <div class="skeleton-line title"></div>
            <div class="skeleton-line meta"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
        </div>
    }
}

#[component]
pub fn PostListSkeleton(#[prop(default = 3)] count: usize) -> impl IntoView {
    view! {
        <div class="post-list-skeleton">
            {(0..count).map(|_| view! { <PostCardSkeleton/> }).collect_view()}
        </div>
    }
}

#[component]
pub fn PostDetailSkeleton() -> impl IntoView {
    view! {
        <div class="post-detail-skeleton">
            <div class="skeleton-line title"></div>
            <div class="skeleton-line meta"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
        </div>
    }
}

#[component]
pub fn SidebarSkeleton() -> impl IntoView {
    view! {
        <div class="sidebar-skeleton">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
            <div class="skeleton-line"></div>
        </div>
    }
}

#[component]
pub fn GenericSkeleton(
    #[prop(default = "100%".to_string())] width: String,
    #[prop(default = "1rem".to_string())] height: String,
) -> impl IntoView {
    view! {
        <div class="skeleton-line" style:width=width style:height=height></div>
    }
}

#[component]
pub fn StaticPageSkeleton() -> impl IntoView {
    view! {
        <div class="static-page-skeleton">
            <div class="skeleton-line title"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
        </div>
    }
}

#[component]
pub fn TagListSkeleton() -> impl IntoView {
    view! {
        <div class="tag-list-skeleton">
            {(0..5)
                .map(|_| view! { <div class="skeleton-line tag-item"></div> })
                .collect_view()}
        </div>
    }
}
