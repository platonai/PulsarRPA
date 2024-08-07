AI Automated Extraction
=======================

[Prev](13X-SQL.md) [Home](1home.md) [Next](15REST.md)

Platon.ai's goal is to develop an AI that efficiently collects and reads complex websites, accurately outputting data 
and knowledge. We have open-sourced the "efficient collection" component. We have also released a preview version that 
"reads and understands webpage structures and accurately outputs data," which will also be open-sourced in the near future.

Platon.ai's algorithm can transform web pages into data with 100% zero human intervention -- without the need for rules, 
and even without machine learning training. It is driven by unsupervised machine learning, similar to how humans read 
and understand the internet.

We calculate a series of features for each element on a webpage after rendering it in a browser, including visual, 
geometric, topological, and semantic features.
**A web page can be considered as a geometric graph composed of many rectangles with attributes, and when 
combined, it resembles a bundle of newspapers. The World Wide Web (WWW) can be viewed as a fiber bundle with a 
three-dimensional manifold as the base space.**

<div style="text-align: center">
    <img width="400px" src=https://pica.zhimg.com/80/v2-1262abb4d28b31a00bcf1199b1aba441_1440w.jpeg?source=d16d100b   alt="auto extracted chart"/>
</div>

You can [download](https://github.com/platonai/PulsarRPAPro#download) and try it:

```
// Given a portal url, automatically extract all the fields from out pages
java -jar exotic-standalone*.jar harvest https://www.hua.com/flower/  -diagnose -refresh
```

Furthermore, given any list page, we can evaluate the linked pages to detect which group of pages is generated by the same template, thus allowing the extraction of field values.

```
// Auto arrange the links in a webpage
java -jar exotic-standalone*.jar arrange https://www.hua.com/flower/ 
```

In this way, the problem of web page extraction that originally required manually writing several or even dozens of 
regular expressions or CSS PATHs can now be solved by simply telling the system the list page link, and web pages that 
meet this requirement account for the vast majority of web pages on the internet.

Finally, we have equipped the crawler system and data analysis system with an SQL engine, so we can monitor a website column and extract key data in real-time with just one SQL statement. In fact, with the SQL engine, the internet and local databases can almost be treated as the same (except for the longer response time of internet data).

<div style="text-align: center">
    <img width="80%" src=https://pic3.zhimg.com/80/v2-dfb9ae6163db8c84b4d7e223c60f8835_1440w.jpg?source=d16d100b   alt="product"/>
</div>

A typical web page section

<div style="text-align: center">
    <img width="80%" src=https://pica.zhimg.com/80/v2-d10694d76cfa5cf148a67c1576ca8f29_1440w.jpg?source=d16d100b   alt="auto extracted data"/>
</div>

Data extracted using PulsarRPA's auto extraction technology

<div style="text-align: center">
    <img width="80%" src=https://pic3.zhimg.com/80/v2-ffe172327bbac5bbc5b43f1ae9d54864_1440w.jpg?source=d16d100b   alt="auto extracted chart"/>
</div>

Using PulsarRPA's auto extraction technology and SQL to fully automate the transformation of the internet into charts

**References:**

- [WebFormer: The Web-page Transformer for Structure Information Extraction | Proceedings of the ACM Web Conference 2022](https://dl.acm.org/doi/pdf/10.1145/3485447.3512032)
- [OpenCeres for extract knowledge graph from Web](https://lunadong.com/publication/openCeres_naacl.pdf)
- [FreeDOM: A Transferable Neural Architecture for Structured Information Extraction on Web Documents](https://arxiv.org/pdf/2010.10755)

**Related Articles:**

- [PlatonAI: How does Diffbot work?](https://zhuanlan.zhihu.com/p/76978950)
- [PlatonAI: How does Plato work?](https://zhuanlan.zhihu.com/p/76980563)

---

[Prev](13X-SQL.md) | [Home](1home.md) | [Next](15REST.md)

