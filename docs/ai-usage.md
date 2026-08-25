These are the raw prompts I used. 
Initially I started my project with ChatGTP, but later realized that it is becoming a big project and manually building files was time taking. So, later I shifted to Claude Code.

ChatGTP

1) "Let's start thinking about this assignment. First focus on requirements, like we do in LLD rounds, focusing on design patterns and SOLID principles wherever they fit."
2) "Clarify what 'answer questions about how the org pays people' means and update the requirements accordingly."
3) "Should we support multiple compensation components such as base pay, bonus and stock units, or keep compensation simple?"
4) "Should we support Excel upload for 10,000 employees? Would it add unnecessary complexity?"


5) "I want to use Java 21 with Spring Boot and Angular. What architecture and dependencies make sense for this application?"
6) "I wouldn't create a separate domain abstraction hierarchy just for the sake of LLD. Don't create unnecessary Factory, Strategy, Adapter or Facade classes. Only use a design pattern when there is a real problem to solve."
7) "Why are we using UUID? Are there any complexity or performance concerns compared with Long?"
8) "Can we use application.properties instead of application.yml? Is there any concern?"


9) "Can two salary records overlap? Explain with an example."
10) "What happens when salary changes mid-month? We need to consider this as an edge case."
11) "Should salary history be immutable?" Yes
12) "Should an employee be deleted when they leave, or should we keep the employee and mark them inactive?" Mark them inactive
13) "Should salary changes effective in the past be allowed?" No
14) "How should analytics handle multiple currencies? Should we use an adapter pattern or another approach?"

15) "Go ahead with code files."
16) "I tested the first few APIs. Go ahead with the next step."
17) "Go ahead with the next step."


Claude code

18) Handover doc from ChatGTP
19) Fix the dependencies error
20) The frontend files structure you build is not considerd as good coding practice, why have you added scss, html and ts in singl file? Separate it.
21) Still, the structure is not we follow generally, Don't you know how to make angular components? Like bar-chart, inside it we should have its file of html, scss, ts and spec, Not all other component files. Why are you making this silly mistakes.
22) There were some styling issues, I fixed it, Now improve the charts visualization, maybe use some external libraries like Highchart or any other, so that we can have tooltips and good looking visualization.

