use leptos::prelude::*;

const WINDOW_SIZE: i32 = 5;

/// Computed pagination state, separated from rendering for testability.
#[derive(Debug, Clone, PartialEq)]
pub struct PaginationCalc {
    pub total_pages: i32,
    pub page_window: Vec<i32>,
    pub has_prev: bool,
    pub has_next: bool,
}

/// Computes pagination window and navigation flags from current state.
/// Returns `None` when there is only one page (pagination should be hidden).
pub fn calculate_pagination(
    current_page: i32,
    total_items: i32,
    items_per_page: i32,
) -> Option<PaginationCalc> {
    let total_pages = (total_items + items_per_page - 1) / items_per_page;
    if total_pages <= 1 {
        return None;
    }
    let half = WINDOW_SIZE / 2;
    let start = (current_page - half).max(1);
    let end = (start + WINDOW_SIZE - 1).min(total_pages);
    let start = (end - WINDOW_SIZE + 1).max(1);
    Some(PaginationCalc {
        total_pages,
        page_window: (start..=end).collect(),
        has_prev: current_page > 1,
        has_next: current_page < total_pages,
    })
}

/// Builds the URL for a given page number.
pub fn page_url(base_url: &str, page: i32) -> String {
    if page == 1 {
        base_url.to_string()
    } else {
        format!("{}?page={}", base_url, page)
    }
}

#[component]
pub fn Pagination(
    current_page: i32,
    total_items: i32,
    items_per_page: i32,
    base_url: String,
) -> impl IntoView {
    let total_pages = (total_items + items_per_page - 1) / items_per_page;

    if total_pages <= 1 {
        return view! {}.into_any();
    }

    let half = WINDOW_SIZE / 2;
    let start = (current_page - half).max(1);
    let end = (start + WINDOW_SIZE - 1).min(total_pages);
    let start = (end - WINDOW_SIZE + 1).max(1);

    let page_url = {
        let base_url = base_url.clone();
        move |page: i32| {
            if page == 1 {
                base_url.clone()
            } else {
                format!("{}?page={}", base_url, page)
            }
        }
    };

    let prev_url = page_url(current_page - 1);
    let next_url = page_url(current_page + 1);

    let pages: Vec<i32> = (start..=end).collect();

    view! {
        <nav class="pagination">
            {if current_page > 1 {
                view! { <a href=prev_url class="pagination-link">"← Prev"</a> }.into_any()
            } else {
                view! { <span class="pagination-link disabled">"← Prev"</span> }.into_any()
            }}
            {pages
                .into_iter()
                .map(|page| {
                    let url = page_url(page);
                    if page == current_page {
                        view! { <span class="pagination-link active">{page}</span> }.into_any()
                    } else {
                        view! { <a href=url class="pagination-link">{page}</a> }.into_any()
                    }
                })
                .collect_view()}
            {if current_page < total_pages {
                view! { <a href=next_url class="pagination-link">"Next →"</a> }.into_any()
            } else {
                view! { <span class="pagination-link disabled">"Next →"</span> }.into_any()
            }}
        </nav>
    }
    .into_any()
}
