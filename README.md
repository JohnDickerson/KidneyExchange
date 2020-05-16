Kidney Exchange
==============

#### What is kidney exchange? ####

Kidney failure is a life-threatening health issue that affects hundreds of thousands of people worldwide. In the US alone, the waitlist for a kidney transplant has over 100,000 patients. This list is growing: demand far outstrips supply.

A recent innovation, kidney exchange, allows patients to bring an (incompatible) donor to a large pool where they can swap donors with other patients.  As of 2012&ndash;2013, roughly 10% of US kidney transplants occurred through a variety of kidney exchanges.  Outside of the US, many countries (the UK, the Netherlands, Portugal, Israel, ...) are fielding exchanges.

#### What is this code? ####

This codebase includes: structural elements of kidney exchange like "pools", "hospitals", and "pairs", a couple of kidney exchange graph generators, a couple of kidney exchange solvers (max weight, failure-aware, fairness-aware, individually rational), and a dynamic kidney exchange simulator.

If you use this codebase, please cite one of our recent papers like:

John P. Dickerson, Ariel D. Procaccia, and Tuomas Sandholm. 2014. Price of Fairness in Kidney Exchange. In _Proceedings of the 2014 International Conference on Autonomous Agents and Multi-agent Systems_ (AAMAS-2014).  Paris, France (pp. 1013&ndash;1020). 

**NOTE:** This is _not_ the code used in the UNOS [Kidney Paired Donation Pilot Program](http://optn.transplant.hrsa.gov/resources/KPDPP.asp "Kidney Paired Donation Pilot Program information via OPTN") (KPDPP).  The solvers here are meant to be accessible research code for the community and do not use branch-and-price, hopefully resulting in greater ease of use (at the cost of scalability).  Forks and pull requests welcome!


External Dependencies
=====================

To use any of the solvers that inherit from `CPLEXSolver`, you will need to add [cplex.jar](http://www-01.ibm.com/software/commerce/optimization/cplex-optimizer/) to `lib/`.  This will allow compilation; to run, you'll also need a VM argument like

   -Djava.library.path=/path/to/CPLEX_Studio/cplex/bin/your-architecture/

IBM offers a free academic license for CPLEX as well as a 90-day free trial available on their website.


Related Research
================

_FutureMatch: Combining Human Value Judgments and Machine Learning to Match in Dynamic Environments_.  John P. Dickerson and Tuomas Sandholm.  _Working Paper_.

_Multi-Organ Exchange: The Whole is Greater than the Sum of its Parts_.  John P. Dickerson and Tuomas Sandholm.  **AAAI-2014**.  [Link](http://jpdickerson.com/pubs/dickerson14multi.pdf "John P. Dickerson")

_The Price of Fairness in Kidney Exchange_.  John P. Dickerson, Ariel D. Procaccia, Tuomas Sandholm.  **AAMAS-2014**.  [Link](http://jpdickerson.com/pubs/dickerson14price.pdf "John P. Dickerson")

_The Empirical Price of Fairness in Failure-Aware Kidney Exchange_.  John P. Dickerson, Ariel D. Procaccia, Tuomas Sandholm.  **AAMAS-2014 Workshop on Healthcare and Algorithmic Game Theory**.  [Link](http://jpdickerson.com/pubs/dickerson14empirical.pdf "John P. Dickerson")

_Failure-Aware Kidney Exchange_.  John P. Dickerson, Ariel D. Procaccia, Tuomas Sandholm.  **EC-2013**.  [Link](http://www.cs.cmu.edu/~sandholm/failure-aware%20kidney%20exchange.ec13.pdf "Carnegie Mellon University link")

_Dynamic Matching via Weighted Myopia with Application to Kidney Exchange_.  John P. Dickerson, Ariel D. Procaccia, Tuomas Sandholm. **AAAI-2012**.  [Link](https://www.cs.cmu.edu/afs/cs.cmu.edu/Web/People/arielpro/papers/weights.aaai12.pdf "Carnegie Mellon University link")

_Optimizing Kidney Exchange with Transplant Chains: Theory and Reality_.  John P. Dickerson, Ariel D. Procaccia, Tuomas Sandholm. **AAMAS-2012**.  [Link](http://www.cs.cmu.edu/afs/cs/Web/People/arielpro/papers/chains.aamas12.pdf "Carnegie Mellon University link")

_Clearing Algorithms for Barter Exchange Markets: Enabling Nationwide Kidney Exchanges_.  David J. Abraham, Avrim Blum, Tuomas Sandholm.  **EC-2007**.  [Link](http://www.cs.cmu.edu/~dabraham/papers/abs07.pdf "Carnegie Mellon University link")

_Increasing the Opportunity of Live Kidney Donation by Matching for Two and Three Way Exchanges_. S. L. Saidman, Alvin Roth, Tayfun S&ouml;nmez, Utku &Uuml;nver, Frank Delmonico.  **Transplantation, 2006**.  [Link](http://kuznets.fas.harvard.edu/~aroth/papers/SaidmanRothSonmezUnverDelmonico.Transplantation.2006.pdf "Harvard link")

_These are admittedly me-centric references!  Make sure to click around: [Ross Anderson](http://rma350.scripts.mit.edu/home/), [Itai Ashlagi](http://web.mit.edu/iashlagi/www/), [Avrim Blum](http://www.cs.cmu.edu/~avrim/), [John Dickerson](http://cs.cmu.edu/~dickerson), [David Gamarnik](http://www.mit.edu/~gamarnik/home.html), [David Parkes](http://www.eecs.harvard.edu/~parkes/), [Ariel Procaccia](http://www.cs.cmu.edu/~arielpro/), [Al Roth](http://www.stanford.edu/~alroth/), [Tuomas Sandholm](http://www.cs.cmu.edu/~sandholm/), [Ankit Sharma](https://sites.google.com/site/ankitsharmahomepage/), [Tayfun S&ouml;nmez](https://www2.bc.edu/~sonmezt/), [Pingzhong Tang](http://iiis.tsinghua.edu.cn/~kenshin/), [Utku &Uuml;nver](https://www2.bc.edu/~unver/)._ 

If you are interested in other social choice problems, make sure to check out [PrefLib](http://www.preflib.org/), a reference library of preference data assembled by [Nicolas Mattei](http://www.nickmattei.net/) and [Toby Walsh](http://www.cse.unsw.edu.au/~tw/).