# 🚗 EduCarpool 

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)]()
[![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)]()
[![Google Maps](https://img.shields.io/badge/Google%20Maps-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white)]()
[![Supabase](https://img.shields.io/badge/Supabase-3FCF8E?style=for-the-badge&logo=supabase&logoColor=white)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)]()
[![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white)]()

---

## ℹ️ About The Project

**EduCarpool** is a dedicated Android mobile application designed to solve the prevalent transport challenges faced by university students. The platform connects verified students pairing passengers with drivers to create secure, cost-effective, and environmentally friendly carpool arrangements for daily commutes to campus.

This system integrates sophisticated mapping and real-time backend technology to offer features such as real-time driver/passenger matching, route visualization, in-app messaging, and formal agreement management.

## 🤝 Project Team & Collaboration

This system was developed through a comprehensive collaborative effort encompassing research, system modeling, architectural design, implementation, and rigorous testing for the **ITMDA3-B34** module.

| Developer | GitHub Profile | Focus Area |
| :--- | :--- | :--- |
| **Luvano Zaal** | https://github.com/Luvano-hub | Core application functionality — developed and implemented the real-time matching algorithm, Google Maps API integration (Geocoding, Directions, and Distance Matrix), and Trip Agreement system, ensuring seamless backend-to-frontend synchronization for dynamic route and agreement management. |
| **Isabel Bocolo** | [https://github.com/IssaBocolo](https://github.com/IssaBocolo) | UI/UX Design — designed and structured the Android app’s user interface layouts, screen navigation, and overall visual consistency, ensuring intuitive user interaction across all dashboards. |
| **Almeerah Losper** | [https://github.com/AlmeerahLosper](https://github.com/AlmeerahLosper) | UI Feedback & Frontend Enhancements — refined user experience components, including responsive elements, dynamic panels, and real-time UI feedback mechanisms within the passenger and driver dashboards. |
| **Thato Maja** | [https://github.com/ThatoMaja](https://github.com/ThatoMaja) | Database Management & Backend Architecture — designed and configured the Supabase PostgreSQL schema, including users, matches, messages, and agreements tables, handling data relationships, queries, and authentication integration. |
| **Connor Coaklin** | [https://github.com/ConnorCoaklin](https://github.com/ConnorCoaklin) | Messaging System Backend — implemented the real-time chat functionality, integrating Supabase Realtime Channels for message synchronization, and developed the backend logic that enables communication between matched users. |

## 🛠️ Technology & Architectural Design

The application is built on a scalable, real-time technology stack that leverages multiple APIs for core functionality.

| Category | Component | Technologies / APIs Used |
| :--- | :--- | :--- |
| **Client (Frontend)** | Mobile Application | Android Studio (Java, XML UI) |
| **Backend & Database** | Realtime Backend, Auth, DB | **Supabase** (PostgreSQL, Realtime API, Auth) |
| **Cloud Hosting** | Infrastructure | Supabase infrastructure (Built on **AWS**) |
| **Mapping & Location** | Geolocation, Routing | Google Maps SDK, Directions API, Geocoding API, Distance Matrix API |
| **Data Sync** | Live Updates | Supabase Realtime Channels |

## 🧠 Core Features & System Breakdown

This table details the core functionalities, from user flow to data management.

| Feature Name | Description & Data Flow | Screenshot |
| :--- | :--- | :--- |
| **User Registration & Auth** | Users register and select their role (`Driver` or `Passenger`). The **Google Geocoding API** converts the address to Latitude/Longitude, which is saved to the Supabase `users` table. | ![Registration Screen](readme-assets/registration.jpg?raw=true&width=250) |
| **Matching Algorithm** | Passengers are matched with drivers within a defined detour radius. The **Google Distance Matrix API** calculates the distance and time in real-time to find optimal matches. | ![Matching View](readme-assets/matching-view.jpg?raw=true&width=250) |
| **Route Visualization** | The **Google Directions API** plots the precise driving route on the map, showing pickup/drop-off points and the final campus destination. | ![Route Visualization](readme-assets/route-visualization.jpg?raw=true&width=250) |
| **Real-Time Requests** | Requests are instantly pushed to the driver's dashboard using **Supabase Realtime Channels**. Drivers Accept/Deny, instantly updating the passenger's status. | ![Realtime Requests](readme-assets/realtime-requests.jpg?raw=true&width=250) |
| **In-App Messaging** | Activated after a match is accepted. Messages are delivered instantly using **Supabase Realtime Channels**, facilitating secure coordination. | ![Messaging Chat](readme-assets/messaging-chat.jpg?raw=true&width=250) |
| **Formal Agreement System** | Allows users to formalize carpool terms: Duration, Price Structure, Times, and Payment Method. The accepted agreement is locked in the `agreements` table. | ![Agreement Proposal](readme-assets/agreement-proposal.jpg?raw=true&width=250) |
| **Dashboard Dynamics** | The UI is highly dynamic, relying on **Supabase Realtime** to auto-refresh components (driver listings, incoming requests) whenever the underlying data changes. | ![Dashboard UI](readme-assets/dashboard-ui.jpg?raw=true&width=250) |

## 🚀 How to Use (High-Level Flow)

1.  **Register:** Select either **Driver** or **Passenger** role and enter your details.
2.  **Login:** Access your role-based dashboard.
3.  **Passenger:** View nearby drivers, select a match, and **Send Request**.
4.  **Driver:** Receive the request notification, view the route on the map, and **Accept** or **Deny**.
5.  **Communicate:** Use the **Messaging System** for coordination.
6.  **Formalize:** Use the **Agreement System** to finalize the terms (price, time, days).

## 📝 Development Notes

* **Context:** This project was developed by the team listed above for the **ITMDA3-B34** module.
* **Scope:** All core features, from real-time matching and routing to the formalized agreement system are fully functional.
* **Future Work:** Planned enhancements include a full **Rating System** (post-agreement completion) better Trip agreement Along with a payment gateway and an Explore page for Students to see campus announcement, assessment reminders and share experiences 

---
## 🔗 Access Links

| Type | Link |
| :--- | :--- |
| **GitHub Repository**| https://github.com/Luvano-hub/EduCarpool |
| **Collaborators** | [IssaBocolo](https://github.com/IssaBocolo) \| [AlmeerahLosper](https://github.com/AlmeerahLosper) \| [ThatoMaja](https://github.com/ThatoMaja) \| [ConnorCoaklin](https://github.com/ConnorCoaklin) |
