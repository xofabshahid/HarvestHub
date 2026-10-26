**HarvestHub**

**Grow Local. Share More. Waste Less.**

Turning surplus harvests into opportunities to nourish communities.

HarvestHub is a Java-based food rescue application designed to connect local farmers with individuals and organizations who can put surplus produce to good use. By combining smart recipient matching, produce freshness analysis, urgency scoring, and delivery route optimization, HarvestHub aims to make food redistribution easier, faster, and more accessible.

Created independently for the **2026 Congressional App Challenge**, HarvestHub explores how technology can help reduce food waste while addressing food insecurity, one harvest at a time!

---

**The Story Behind HarvestHub**

Growing up in Waller, Texas, I've always been surrounded by farmland and agriculture. Yet even in a community where food is grown, many families face food insecurity.

Through volunteering with the Waller ISD School Market, operated in partnership with the Houston Food Bank, I've seen how much of a difference food distribution programs make for local families.

That experience inspired a question:

*What if we could make it easier for farmers to share surplus produce with the people who need it?*

That's how HarvestHub began.

My goal was to build something that could bridge the gap between agricultural surplus and community needs, using technology to make food rescue more efficient and accessible.

---

**Features**

**HarvestScan — Produce Freshness Analysis**

Upload an image of produce to receive a visual assessment of potential spoilage risk.

- Analyzes visible produce characteristics using image-processing techniques
- Calculates pixel-based indicators to estimate visual risk
- Provides an additional signal for determining redistribution urgency

**Harvest Helper — Smart Urgency Scoring**

Not every harvest has the same amount of time before it needs to be redistributed!

Harvest Helper calculates an urgency score from 0–100 using factors such as:

- Produce and harvest information
- Storage conditions
- Current weather
- Optional HarvestScan results

It then recommends a pickup window to help prioritize time-sensitive produce.

**Smart Match — Finding the Right Recipient**

HarvestHub doesn't simply recommend the closest recipient. It considers whether that recipient is actually a good fit for the available harvest.

Its weighted matching algorithm evaluates:

| Factor | Weight |
|---|---:|
| Distance | 25% |
| Quantity compatibility | 25% |
| Pickup timing | 20% |
| Crop compatibility | 15% |
| Donation compatibility | 15% |

This approach helps balance logistical efficiency with the needs and capabilities of potential recipients.

**Route Optimization — Making Every Trip Count**

Once recipients are identified, HarvestHub helps plan an efficient delivery route.

Using a nearest-neighbor heuristic, the application:

1. Starts at the farmer's location.
2. Identifies the nearest unvisited recipient.
3. Updates the current location after each stop.
4. Repeats until the route is complete.
5. Displays the route on a map.

This provides a practical way to organize multiple deliveries without evaluating every possible route combination.

**Produce Marketplace**

A place to connect surplus produce with interested recipients.

- Browse available harvest listings
- List produce for donation or sale
- Reserve available produce
- Manage listings through My Farm

**Impact Dashboard**

HarvestHub also includes an impact dashboard designed to help users visualize their food rescue activity and contributions.

---

**Built With**

- Java — Application development and core logic
- Computer vision techniques — Pixel-based produce image analysis
- Weighted scoring algorithms — Recipient matching and harvest urgency
- Geographic distance calculations — Delivery route planning
- Nearest-neighbor heuristic — Efficient multi-stop routing

HarvestHub contains 5,000+ lines of Java code, independently written, tested, and refined over several months.

---

**Challenges & Lessons Learned**

Building HarvestHub meant solving problems that went far beyond writing code.

One challenge was designing Smart Match. My initial approach focused too heavily on distance, but I realized that the nearest recipient might not be able to accept most of a harvest. After exploring different scenarios and learning from people involved in local food distribution, I developed a weighted system that considers several factors together.

Route optimization presented another challenge. With just ten destinations, there are 3,628,800 possible delivery orders! Researching routing algorithms led me to implement a nearest-neighbor approach that offers a practical solution without checking every possible route.

Through this project, I learned that good technology isn't just about making algorithms work. It's about making them work for people.

---

**What's Next?**

HarvestHub is a starting point, and I'm excited about where it could go!

Some future improvements I'd love to explore include:

- Training a machine learning model for more advanced produce freshness detection
- Refining Smart Match using real-world recipient feedback
- Incorporating traffic conditions, delivery time windows, and vehicle capacity into route planning
- Testing the application with local farmers and community organizations
- Expanding food rescue connections beyond Waller

---

**Meet the Developer**

Hi! I'm Fatimah Shahid, a student at Waller High School in Waller, Texas!

I developed HarvestHub independently as my submission to the 2026 Congressional App Challenge.

From brainstorming the initial concept to writing the Java code, building the algorithms, testing features, creating presentation slides, and recording the demonstration video, this project has been a labor of love over several months.

As someone passionate about computer science, innovation, and community service, I wanted to create something that combined my technical interests with a cause close to home.

HarvestHub has taught me so much about programming, persistence, and the importance of listening to the communities we hope to serve.

I'm excited to keep learning, building, and finding new ways to use technology for good!

---

**A Little Thank You**

Thank you to the teachers, student volunteers, and community members involved in the Waller ISD School Market and Houston Food Bank partnership. Your dedication to serving local families continues to inspire me.

And thank you for taking the time to explore HarvestHub!

---

**Grow Local. Share More. Waste Less.**

*Because good food deserves to be shared, not wasted.*

Made in Waller, Texas.
