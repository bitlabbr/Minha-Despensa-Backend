# Contributing to Minha Despensa Backend

Thank you for your interest in contributing to **Minha Despensa Backend**!

This project is released under the **Creative Commons Attribution-NonCommercial 4.0 International License (CC BY-NC 4.0)**.
By contributing, you agree to the terms described below.

---

## 1. 📜 License and Rights

- This project is **free for non-commercial use only**.
- You may use, modify, and redistribute the code for **non-commercial purposes**, provided proper attribution is given.
- **Commercial use is prohibited** without explicit written permission from the author.

### Copyright Holder Rights
- The copyright holder (**Willian Santos**) retains **all commercial rights**.
- The author may use the project commercially, sell or sublicense the code, or relicense the project under different terms at any time.

---

## 2. 📝 Contributor License Agreement (CLA)

By submitting a contribution (code, pull requests, documentation, or infrastructure configurations), you agree that:
1. Your contribution is your own original work or you have the legal right to submit it.
2. You grant the project author a **perpetual, worldwide, royalty-free, irrevocable right** to use, modify, relicense, and distribute your contribution.
3. Your contribution will be licensed under the same **CC BY-NC 4.0 license** as the rest of the project.

---

## 3. 🏛️ Architecture & Code Standards

Contributors must adhere to the core architectural guidelines outlined in [`Minha-Despensa-Backend.md`](./Minha-Despensa-Backend.md):

1. **Modular Monolith Principles:**
   - Keep business logic encapsulated within its vertical module under `modules/<domain>`.
   - Cross-cutting concerns belong strictly to `common/`.
2. **Local-First & CRDT Last-Write-Wins (LWW):**
   - Synchronizable entities must inherit from `BaseSyncEntity` (`id: UUID`, `updatedAt: Long`, `isDeleted: Boolean`).
   - Soft-delete is mandatory: never execute physical SQL deletes for synchronizable records.
3. **Jackson 3 Deserialization Conventions:**
   - Do **NOT** use `@field:JsonAlias` or `@field:JsonProperty` on primary constructor properties of Kotlin data classes.
   - Annotate the constructor parameter directly (`@JsonAlias("nome")`).
4. **Mandatory License Header:**
   - All `.kt` source and test files must begin with the standard CC BY-NC 4.0 copyright header.
5. **Observability & Request Logging:**
   - Keep endpoint requests audited through `RequestLoggingFilter`.
   - Log 4xx client errors as `WARN` and 5xx unexpected failures as `ERROR`.

---

## 4. 🧪 Automated Testing Policy

- **100% Green Tests:** All unit and WebMvc tests must pass locally before opening a pull request:
  ```bash
  ./gradlew test
  ```
- Any new business logic must include unit tests with Mockito Kotlin.
- Any new REST endpoint must include `@WebMvcTest` contract validation.

---

## 5. 🚀 How to Contribute

1. **Fork the repository** on GitHub.
2. **Create a branch** based on `dev`:
   ```bash
   git checkout dev
   git pull origin dev
   git checkout -b feat/your-feature-name
   # or
   git checkout -b fix/your-bugfix-name
   ```
3. **Implement your changes** following the code and header standards.
4. **Run the full test suite and build**:
   ```bash
   ./gradlew check
   ./gradlew bootJar -x test
   ```
5. **Commit using Conventional Commits**:
   - `feat(catalog): add bulk import endpoint`
   - `fix(sync): correct timestamp tie-break rule`
   - `test(pantry): add occurrence aggregation test`
   - `docs(backend): update API contract table`
6. **Open a Pull Request targeting `dev`** with a clear explanation of what was built and how it was tested.

---

## 💬 Commercial Licensing & Partnerships

If you are interested in commercial usage, partnerships, or bespoke licensing terms, please contact the author directly.

© 2026 Willian Santos
