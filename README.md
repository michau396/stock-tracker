# Stock Portfolio Tracker

A professional Spring Boot application for tracking stock portfolios with real-time price updates, portfolio distribution charts, and Google OAuth 2.0 authentication.

## Tech Stack

- Java 21/23
- Spring Boot 3.2.0
- Maven
- Finnhub API
- Spring WebFlux
- Thymeleaf for templating
- H2 Database (file-based)
- Spring Security + OAuth2
- JPA/Hibernate
- Chart.js for data visualization
- html2pdf.js for PDF export

## Features

- **Google OAuth 2.0 Authentication**: Secure login with Google account
- **Portfolio Management**: Add, view, and delete stock positions
- **Purchase Date Tracking**: Track when each position was purchased
- **Real-time Price Updates**: Auto-updates stock prices once per day
- **Portfolio Distribution**: Visual pie chart showing portfolio allocation
- **Portfolio Value Over Time**: Line chart with week/month/year views
- **Profit/Loss Tracking**: Automatic calculation of profit/loss for each position
- **Total Portfolio Value**: Aggregated view of your entire portfolio worth
- **PDF Export**: One-click PDF generation of portfolio summary
- **Autocomplete**: Smart stock symbol search with autocomplete
- **Persistent Storage**: H2 file-based database for data persistence
- **Professional UI**: Modern, gradient-themed interface with glassmorphism effects

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Google Cloud Console project with OAuth 2.0 credentials

## How to Run

1. **Configure Google OAuth 2.0**:
   - Create a project in [Google Cloud Console](https://console.cloud.google.com/)
   - Enable Google+ API
   - Create OAuth 2.0 client credentials (Web application)
   - Add authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
   - Update client ID and secret in `application.properties`

2. **Build and run**:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

3. **Access the application**:
   ```
   http://localhost:8080
   ```

## Getting a Finnhub API Key

1. Go to [Finnhub](https://finnhub.io/) and sign up for a free account
2. Navigate to the API Key section in your dashboard
3. Copy your API key and update it in `application.properties`

A default API key is already configured, so you can start using the app immediately.

## Usage

1. **Login**: Sign in with your Google account
2. **Add Position**: 
   - Enter a stock symbol (with autocomplete support)
   - Specify quantity, purchase price, and purchase date
   - Click "Add Position" to add to your portfolio
3. **View Portfolio**:
   - See all your stock positions with real-time prices
   - View total portfolio value and profit/loss
   - See portfolio distribution via pie chart
   - Track portfolio value over time with line chart
4. **Generate PDF**: Click "Generate PDF" to download portfolio summary
5. **Manage Positions**: Delete positions you no longer want to track

## Database

The application uses H2 file-based database stored at `./stocktracker.mv.db` in the project root. Data persists between application restarts.

## Notes

- Finnhub free tier allows 60 requests per minute
- Stock prices update automatically once per day on app startup
- Purchase dates are used for accurate portfolio history calculations
- PDF export includes charts, tables, and summary metrics
- Google OAuth users are auto-saved to database on first login
