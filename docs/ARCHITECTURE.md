# Luv2Shop — Architecture

A full-stack e-commerce reference app: an **Angular 21** single-page storefront talking to a
**Spring Boot 4.1** REST API backed by **MySQL 8**, with **Okta** (auth) and **Stripe** (payments)
as optional, gracefully-degrading integrations.

- [System context](#1-system-context)
- [Backend architecture](#2-backend-architecture)
- [Frontend architecture](#3-frontend-architecture)
- [Data model](#4-data-model)
- [Key flows](#5-key-flows)
- [Cross-cutting concerns](#6-cross-cutting-concerns)
- [Tech stack](#7-tech-stack)

---

## 1. System context

How the pieces fit together and what crosses each boundary.

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'DM Sans, system-ui, sans-serif','lineColor':'#9aa3b8','clusterBkg':'#f5f7fd','clusterBorder':'#e7ecf7','clusterTextColor':'#1e2435'}}}%%
flowchart LR
  user([🛍️  Shopper]):::user

  subgraph Browser["🖥️  Browser · localhost:4250"]
    spa["Angular 21 SPA<br/>standalone components"]:::fe
  end

  subgraph Cloud["☁️  Server · localhost:8585"]
    api["Spring Boot 4.1 API<br/>Spring Data REST + MVC"]:::be
  end

  db[("🗄️  MySQL 8<br/>full-stack-ecommerce")]:::db
  okta{{"🔐  Okta · OIDC<br/>(optional)"}}:::ext
  stripe{{"💳  Stripe · Payments<br/>(optional)"}}:::ext

  user -->|clicks| spa
  spa -->|"REST / JSON over HTTP · CORS"| api
  api -->|"JPA / JDBC"| db
  spa -.->|"OIDC redirect login"| okta
  api -.->|"validates JWT"| okta
  spa -.->|"card tokenization (Elements)"| stripe
  api -.->|"creates PaymentIntent"| stripe

  classDef user fill:#1e2435,stroke:#1e2435,color:#ffffff;
  classDef fe fill:#ffe3ea,stroke:#ff5470,color:#1e2435;
  classDef be fill:#d3f3ef,stroke:#10b6a6,color:#1e2435;
  classDef db fill:#fff4e0,stroke:#f5b400,color:#1e2435;
  classDef ext fill:#eef2fb,stroke:#9aa3b8,color:#1e2435;
```

> **Graceful degradation:** Okta and Stripe are optional. Without them the app still builds and
> runs — login pages stay viewable (demo mode) and checkout skips the card step and saves the order
> directly. See [STRIPE.md](STRIPE.md) and [BUILD_PLAN.md](BUILD_PLAN.md) §M3.

---

## 2. Backend architecture

A classic layered Spring Boot app. Most read endpoints are **auto-generated** by Spring Data REST
straight from the repositories; only checkout is a hand-written controller.

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'DM Sans, system-ui, sans-serif','lineColor':'#9aa3b8','clusterBkg':'#f5f7fd','clusterBorder':'#e7ecf7','clusterTextColor':'#1e2435'}}}%%
flowchart TD
  subgraph web["🌐  Web / API layer"]
    rest["Spring Data REST<br/>/api/products · /api/product-category<br/>/api/countries · /api/states · /api/orders"]:::be
    cc["CheckoutController<br/>/api/checkout/purchase<br/>/api/checkout/payment-intent"]:::be
  end
  subgraph cfg["⚙️  Config (cross-cutting)"]
    cors["MyDataRestConfig<br/>CORS · read-only · expose ids"]:::cfg
    sec["SecurityConfig<br/>conditional JWT resource server"]:::cfg
  end
  subgraph svc["🧮  Service layer"]
    chk["CheckoutServiceImpl<br/>order assembly · Stripe · tracking #"]:::be
  end
  subgraph data["💾  Data layer"]
    repos["JpaRepository&lt;T&gt;<br/>Product · ProductCategory · Order<br/>Customer · Country · State"]:::be
    ent["JPA Entities (@Entity)"]:::be
  end
  seed["DataLoader<br/>CommandLineRunner — seeds catalog + geo"]:::accent
  db[("MySQL")]:::db

  rest --> repos
  cc --> chk --> repos
  repos --> ent --> db
  seed --> repos
  cors -. configures .-> rest
  sec -. guards .-> rest

  classDef be fill:#d3f3ef,stroke:#10b6a6,color:#1e2435;
  classDef cfg fill:#eef2fb,stroke:#7c5cff,color:#1e2435;
  classDef accent fill:#ffe3ea,stroke:#ff5470,color:#1e2435;
  classDef db fill:#fff4e0,stroke:#f5b400,color:#1e2435;
```

**Package layout** (`com.bob.ecommerceangularapp`):

| Package | Responsibility |
|---|---|
| `entity` | JPA entities — `Product`, `ProductCategory`, `Order`, `OrderItem`, `Customer`, `Address`, `Country`, `State` |
| `dao` | Spring Data `JpaRepository` interfaces (most auto-exposed as REST) |
| `dto` | `Purchase`, `PurchaseResponse`, `PaymentInfo` |
| `service` | `CheckoutService` / `CheckoutServiceImpl` |
| `controller` | `CheckoutController` |
| `config` | `MyDataRestConfig` (CORS + exposure), `SecurityConfig` (JWT) |
| `bootstrap` | `DataLoader` (seed data) |

---

## 3. Frontend architecture

Standalone Angular components → injectable services → `HttpClient` → REST API. A functional HTTP
interceptor attaches the Okta bearer token to the secured order endpoints.

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'DM Sans, system-ui, sans-serif','lineColor':'#9aa3b8','clusterBkg':'#f5f7fd','clusterBorder':'#e7ecf7','clusterTextColor':'#1e2435'}}}%%
flowchart TD
  subgraph cmp["🧩  Components"]
    pl["ProductList · ProductDetails<br/>Search · ProductCategoryMenu"]:::fe
    cart["CartStatus · CartDetails · Checkout<br/>OrderConfirmation"]:::fe
    sys["Toast · ThemeToggle · BackToTop<br/>LoginStatus · OrderHistory"]:::fe
  end
  subgraph srv["🛠️  Services (singletons)"]
    psvc["ProductService"]:::svc
    csvc["CartService<br/>(signals + sessionStorage)"]:::svc
    cksvc["CheckoutService · Luv2ShopFormService"]:::svc
    tsvc["ToastService · ThemeService"]:::svc
  end
  http["HttpClient<br/>+ authInterceptor"]:::core
  api["Spring Boot API"]:::be

  pl --> psvc
  cart --> csvc
  cart --> cksvc
  sys --> tsvc
  psvc --> http
  cksvc --> http
  http -->|"/api/**"| api

  classDef fe fill:#ffe3ea,stroke:#ff5470,color:#1e2435;
  classDef svc fill:#d3f3ef,stroke:#10b6a6,color:#1e2435;
  classDef core fill:#eef2fb,stroke:#7c5cff,color:#1e2435;
  classDef be fill:#fff4e0,stroke:#f5b400,color:#1e2435;
```

Routes: `/products`, `/products/:id`, `/category/:id`, `/search/:keyword`, `/cart-details`,
`/checkout`, `/order-confirmation/:trackingNumber`, `/members/orders` (guarded), `/login/callback`,
`**` → 404.

---

## 4. Data model

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'DM Sans, system-ui, sans-serif','lineColor':'#9aa3b8'}}}%%
erDiagram
  PRODUCT_CATEGORY ||--o{ PRODUCT : groups
  COUNTRY ||--o{ STATE : contains
  CUSTOMER ||--o{ ORDERS : places
  ORDERS ||--o{ ORDER_ITEM : contains
  ORDERS ||--|| ADDRESS : "ships to"
  ORDERS ||--|| ADDRESS : "bills to"

  PRODUCT {
    bigint id PK
    string sku
    string name
    decimal unit_price
    int units_in_stock
    bigint category_id FK
  }
  PRODUCT_CATEGORY {
    bigint id PK
    string category_name
  }
  ORDERS {
    bigint id PK
    string order_tracking_number
    decimal total_price
    int total_quantity
    bigint customer_id FK
  }
  ORDER_ITEM {
    bigint id PK
    bigint product_id
    int quantity
    decimal unit_price
    bigint order_id FK
  }
  CUSTOMER {
    bigint id PK
    string email
  }
  ADDRESS {
    bigint id PK
    string street
    string city
    string state
    string country
  }
  COUNTRY {
    int id PK
    string code
  }
  STATE {
    int id PK
    string name
    int country_id FK
  }
```

The exact DDL Hibernate generates is in [`backend/schema.sql`](../backend/schema.sql).

---

## 5. Key flows

### 5a. Browse the catalog

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'DM Sans, system-ui, sans-serif','actorBkg':'#ff5470','actorTextColor':'#fff','actorBorder':'#ec3a5c','signalColor':'#1e2435','labelBoxBkgColor':'#eef2fb','labelBoxBorderColor':'#e7ecf7','noteBkgColor':'#fff4e0','noteBorderColor':'#f5b400'}}}%%
sequenceDiagram
  autonumber
  participant U as Shopper
  participant PL as ProductList
  participant PS as ProductService
  participant API as Spring Data REST
  participant DB as MySQL

  U->>PL: open /products
  PL->>PS: getProductListPaginate(page,size,catId)
  PS->>API: GET /api/products/search/findByCategoryId
  API->>DB: SELECT ... LIMIT ?,?
  DB-->>API: rows
  API-->>PS: { _embedded.products, page }
  PS-->>PL: products + pagination
  PL-->>U: grid + paginator (staggered reveal)
```

### 5b. Checkout & save order

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'DM Sans, system-ui, sans-serif','actorBkg':'#10b6a6','actorTextColor':'#fff','actorBorder':'#0a857a','signalColor':'#1e2435','labelBoxBkgColor':'#eef2fb','labelBoxBorderColor':'#e7ecf7','noteBkgColor':'#fff4e0','noteBorderColor':'#f5b400'}}}%%
sequenceDiagram
  autonumber
  participant U as Shopper
  participant CO as Checkout (Angular)
  participant ST as Stripe
  participant API as CheckoutController
  participant SVC as CheckoutService
  participant DB as MySQL

  U->>CO: Place order
  opt Stripe configured
    CO->>API: POST /api/checkout/payment-intent
    API->>ST: PaymentIntent.create(amount)
    ST-->>API: client_secret
    API-->>CO: client_secret
    CO->>ST: confirmCardPayment(client_secret, card)
    ST-->>CO: success
  end
  CO->>API: POST /api/checkout/purchase
  API->>SVC: placeOrder(purchase)
  SVC->>DB: cascade save customer → order → items → addresses
  DB-->>SVC: ids
  SVC-->>API: tracking number
  API-->>CO: { orderTrackingNumber }
  CO-->>U: /order-confirmation/:trackingNumber
```

---

## 6. Cross-cutting concerns

| Concern | How it's handled |
|---|---|
| **CORS** | `MyDataRestConfig` + `@CrossOrigin` allow the Angular origin(s) (`:4200`, `:4250`) |
| **Security** | `SecurityConfig` — JWT resource server, *active only* when an Okta issuer is configured; otherwise an open chain. Protects `GET /api/orders/**`. |
| **Pagination** | Spring Data REST HAL envelope: `_embedded.<rel>` + `page:{number,size,totalElements,totalPages}` |
| **Seeding** | `DataLoader` (`CommandLineRunner`) — idempotent, only seeds an empty DB |
| **State (FE)** | `CartService` uses Angular signals + `sessionStorage`; `ThemeService` persists theme |
| **Notifications** | `ToastService` (signal-driven) + `<app-toast>` hub |
| **Config** | MySQL via docker-compose; secrets (Stripe/Okta) via env vars, never committed |

---

## 7. Tech stack

| Layer | Technology |
|---|---|
| Frontend | Angular 21 (standalone), TypeScript, Bootstrap 5, ng-bootstrap, Font Awesome, Stripe.js, Okta Angular |
| Backend | Spring Boot 4.1, Spring Data JPA + REST, Spring Security (OAuth2 resource server), Spring MVC, Lombok, stripe-java |
| Data | MySQL 8 (prod/dev), H2 (tests) |
| Build / tooling | Maven (wrapper), Angular CLI, Vitest, JUnit 5, Docker Compose |

See also: **[API.md](API.md)** · **[DEVELOPMENT.md](DEVELOPMENT.md)** · **[STRIPE.md](STRIPE.md)** · **[BUILD_PLAN.md](BUILD_PLAN.md)**
