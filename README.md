# HarvestHub

**Grow Local. Share More. Waste Less.**

HarvestHub is a Java-based application I developed for the 2026 Congressional App Challenge to help connect local farmers with people and organizations who can use their surplus produce. It brings together produce listings, freshness analysis, recipient matching, and delivery planning to make food redistribution more efficient and accessible.

## Why I Built HarvestHub

I live in Waller, Texas, a community surrounded by farms and agricultural activity. Despite this, food insecurity remains a challenge for many local families.

I've volunteered with the Waller ISD School Market, which partners with the Houston Food Bank to distribute groceries to families in our community. Being involved in these distributions helped me understand how important food access is and inspired me to think about ways technology could support these efforts.

I started wondering how farmers with extra produce could connect more easily with people who could use it. That question eventually became HarvestHub.

## What HarvestHub Does

**HarvestScan**

HarvestScan allows users to upload an image of their produce and receive a visual assessment of potential spoilage risk. I developed an image-processing approach that analyzes pixel-level characteristics to identify possible freshness concerns.

**Harvest Helper**

Harvest Helper helps farmers determine how urgently their produce should be redistributed. It considers harvest information, storage conditions, current weather, and optional HarvestScan results to calculate an urgency score from 0 to 100 and recommend a pickup window.

**Smart Match**

Smart Match recommends recipients based on more than just proximity. I designed a weighted scoring algorithm that considers five factors:

| Factor | Weight |
|---|---|
| Distance | 25% |
| Quantity compatibility | 25% |
| Pickup timing | 20% |
| Crop compatibility | 15% |
| Donation compatibility | 15% |

The goal is to identify recipients who are not only nearby but also capable of accepting the available produce.

**Route Optimization**

HarvestHub also helps farmers plan deliveries to multiple recipients. I implemented a nearest-neighbor algorithm that calculates geographic distances, selects the closest unvisited destination, and continues until all stops have been included.

The application also displays the delivery route on a map.

**Marketplace and Farm Management**

Farmers can create listings for surplus produce, offer items for donation or sale, and manage their available harvests. Recipients can browse listings and reserve produce.

**Impact Dashboard**

The impact dashboard helps users view their food rescue activity and understand their contributions to reducing food waste.

## How I Built It

I developed HarvestHub entirely in Java, writing more than 5,000 lines of code over several months.

The project involved implementing image-processing techniques, designing mathematical scoring systems, working with geographic distance calculations, and connecting multiple features into one application.

Two of the most challenging parts were Smart Match and route optimization.

When I first worked on Smart Match, I focused too heavily on distance. Through testing different scenarios, I realized that the nearest recipient might only be able to accept a small portion of a harvest, while another recipient farther away could take much more. I adjusted the algorithm to consider quantity, timing, and compatibility alongside distance.

For route optimization, I researched approaches to the Traveling Salesperson Problem, including Google's OR-Tools documentation. Rather than checking every possible delivery order, I implemented my own nearest-neighbor heuristic in Java.

Both challenges taught me how important it is to question my initial assumptions, research possible solutions, and keep testing until the results make sense.

## Future Plans

There are several improvements I'd like to explore as I continue developing HarvestHub:

- Improve HarvestScan using a trained machine learning model and a larger produce image dataset.
- Refine Smart Match using feedback from farmers and recipients.
- Add traffic conditions, delivery time windows, and vehicle capacity to route planning.
- Test HarvestHub with local farmers and food distribution organizations.
- Explore ways to expand the application beyond Waller.

## About Me

Hi! I'm Fatimah Shahid, a student at Waller High School in Waller, Texas.

I created HarvestHub independently for the 2026 Congressional App Challenge. I designed and programmed the application, developed and tested its algorithms, created the presentation materials, and recorded the demonstration video myself.

This project combines two things I care deeply about: computer science and community service. Building HarvestHub has challenged me to become a better programmer, but it has also encouraged me to become more involved in my community and think more seriously about how technology can address everyday problems.

I'm excited to continue learning and improving HarvestHub!

## Acknowledgments

Thank you to the teachers, students, and community volunteers involved in the Waller ISD School Market and its partnership with the Houston Food Bank. Your work supporting local families has been a major inspiration for this project.

Thank you for checking out HarvestHub!

**Grow Local. Share More. Waste Less.**
