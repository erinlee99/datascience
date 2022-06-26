
* H&M Personalized Recommendation Algorithm

 
**Problem:**
It is often difficult for customers to find what they are looking for and ultimately may not make a purchase, especially at large-scale websites such as H&M. Helping customers make the right choice would not only save the customer’s time and serve their needs but also benefit the seller by increasing their sales. Also, making the right purchase choices reduces returns and minimizes transportation emissions and packaging waste. Thus, to enhance the shopping experience, product recommendations are essential.
 
**Goal:**
To develop accurate product recommendations based on data from previous transactions, customer meta data, and product meta data.
 
**Data:**
meta data spanning from simple data (i.e. garment type, customer age) to text data from product descriptions, to image data from garment images.
 
**Process:**

Brainstorming and data preparation:
-	The data that we use projects were “articles”, “customers”, and “transaction”.
articles: contains metadata of the all products(clothes) that H&M slls.
customers: contains metadata of the all customers of H &M
transaction: contains the timesereis data of products purchase.
	- 	First, our team construct a mindmap that represents the interconnectivity relationship between 
those data that wraps the whole information.
 
EDA/Data Cleaning:
-    	Created a function to split the data up into 10 CSVs. Because the data was too large to be run on local Jupyter notebooks (6.8M+ unique customer IDs

-    	Created a function that  generates a dataframe that merges all distinct product types purchased for each customer.
-    	Decided to reduce the dimensionality of our data by considering product type, which was a larger category including article ids, rather than considering each distinct            
-    	Grouped the age feature into 4 age groups: 10, 20, 30 40, and created a function that returns a dictionary with the top 50 sold items for each age group.
 
Modeling:
-    	Constructed a decoding algorithm that quantified the similarity between consumption patterns of each postal codes using one hot encoding, clustering. Then, we used this to create a similarity variable that  to improve model performance.
-    	Used AWS cloud & Amazon SageMaker for the modeling process, because the data was too large to be modeled locally.
-    	Developed a K-Means Clustering recommendation algorithm for 6.8M+ unique customer ids utilizing AWS S3, SageMaker, and Python.
o   Chose to use K-Means Clustering because we predicted that it would be appropriate for creating clusters based on the similarities between each customer, with the customer IDs as centroids.
 
**Evaluation**
 
Limitations:
A better choice of model may have been hierarchical clustering, which is agglomerative clustering in scikit-learn. Hierarchical clustering uses a bottom-up approach to sequentially merge similar clusters. The algorithm groups observations that are close to each other until all the observations are joined at the top of the hierarchy. However, we could not use this as this was not offered as a model on Amazon SageMaker. Had we had more time and capacity, we could have tried more models such as hierarchical clustering.
 
The number of features we had to fit the data was small, with items such as age, club member status, fashion news frequency, postal code, and the corresponding purchased product IDs and product types, while the unique number of customer IDs that we had to fit the data to was incredibly large. It was therefore difficult achieving high accuracy with our model.
