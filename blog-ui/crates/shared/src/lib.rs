use yew::prelude::*;
use yew_router::history::{AnyHistory, History, MemoryHistory};
use yew_router::prelude::*;

#[function_component(Home)]
pub fn home() -> Html {
    html! {
        <div>
            <h3>{ "Home" }</h3>
            <Link<Route> to={Route::Blog}>{ "Go to blog" }</Link<Route>>
        </div>
    }
}

#[function_component(Blog)]
pub fn blog() -> Html {
    html! {
        <div>
            <h3>{ "Blog" }</h3>
            <Link<Route> to={Route::Home}>{ "Go to home" }</Link<Route>>
        </div>
    }
}

#[derive(Clone, Routable, PartialEq)]
enum Route {
    #[at("/")]
    Home,
    #[at("/blog")]
    Blog
}

fn switch(routes: Route) -> Html {
    match routes {
        Route::Home => html! {
            <Home />
        },
        Route::Blog => html! {
            <Blog />
        }
    }
}

#[function_component(App)]
pub fn app() -> Html {
    return html! {
        <BrowserRouter>
            <Switch<Route> render={switch} />
        </BrowserRouter>
    }
}

#[derive(Properties, PartialEq, Debug)]
pub struct ServerAppProps {
    pub url: AttrValue,
}

#[function_component(ServerApp)]
pub fn server_app(props: &ServerAppProps) -> Html {
    let history = AnyHistory::from(MemoryHistory::new());
    history.push(&*props.url);

    return html! {
        <Router history={history}>
            <Switch<Route> render={switch} />
        </Router>
    };
}
